package work.nemonet.littlemaidneo.resource.classloader;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MultiModelClassLoader extends URLClassLoader {
    private final MultiModelClassTransformer transformer = new MultiModelClassTransformer();

    public MultiModelClassLoader(List<Path> folderPaths) {
        super(getClassLoaderURL(folderPaths), MultiModelClassLoader.class.getClassLoader());
    }

    private static URL[] getClassLoaderURL(List<Path> folderPaths) {
        List<URL> urlList = new ArrayList<>();
        folderPaths.forEach(folderPath -> {
            try {
                try (var stream = Files.walk(folderPath)) {
                    stream.filter(resourcePath -> !Files.isDirectory(resourcePath))
                            .map(resourcePath -> {
                                try {
                                    return resourcePath.toUri().toURL();
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            })
                            .forEach(urlList::add);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return urlList.toArray(new URL[0]);
    }

    @Override
    protected Class<?> findClass(final String className) throws ClassNotFoundException {
        InputStream inputstream = this.getResourceAsStream(className.replace(".", "/") + ".class");
        if (inputstream == null) {
            throw new ClassNotFoundException(className + ":inputstream:" + className.replace(".", "/") + ".class");
        }
        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(inputstream);
        } catch (Exception e) {
            throw new ClassNotFoundException(className + ":toByteArray[" + e + "]");
        }
        if (bytes == null) {
            throw new ClassNotFoundException(className + ":bytes");
        }
        byte[] transBytes = transformer.transform(bytes);
        try {
            return defineClass(className, transBytes, 0, transBytes.length);
        } catch (Exception e) {
            throw new ClassNotFoundException(className + ":defineClass_Exception:[" + e + "]");
        } catch (Error e) {
            throw new ClassNotFoundException(className + ":defineClass_Error:[" + e + "]");
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (IndexOutOfBoundsException e) {
            if (name.lastIndexOf('.') == -1) {
                Class<?> c = findClass(name);
                if (resolve) resolveClass(c);
                return c;
            }
            throw new ClassNotFoundException(name);
        }
    }
}
