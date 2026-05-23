package work.nemonet.littlemaidneo.resource.classloader;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.neoforged.fml.loading.FMLEnvironment;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MultiModelClassTransformer {
    private static final String PACKAGE_STRING = "work/nemonet/littlemaidneo/maidmodel/";

    private static final Map<String, String> CODE_REPLACE_MAP = new Object2ObjectOpenHashMap<>() {
        {
            String caps = "firis/lmmm/api/caps/";
            addTransformTarget("IModelCaps");
            addTransformTarget("IModelCaps", caps);
            put("mmmlibx/lib/MMM_EntityCaps", "work/nemonet/littlemaidneo/maidmodel/EntityCaps");
            put("littleMaidMobX/EntityCaps", "work/nemonet/littlemaidneo/maidmodel/EntityCaps");
            put("net/blacklab/lmr/util/EntityCaps", "work/nemonet/littlemaidneo/maidmodel/EntityCaps");
            put("net/blacklab/lmr/util/EntityCapsLiving", "work/nemonet/littlemaidneo/maidmodel/EntityCaps");

            addTransformTarget("ModelCapsHelper");
            addTransformTarget("ModelCapsHelper", caps);

            String renderer = "firis/lmmm/api/renderer/";
            addTransformTarget("ModelRenderer");
            addTransformTarget("ModelRenderer", renderer);

            String modelParts = "firis/lmmm/api/model/parts/";
            addTransformTarget("ModelBoxBase");
            addTransformTarget("ModelBoxBase", modelParts);
            addTransformTarget("ModelBox");
            addTransformTarget("ModelBox", modelParts);
            addTransformTarget("ModelPlate");
            addTransformTarget("ModelPlate", modelParts);

            String builtinModel = "firis/lmmm/builtin/model/";
            addTransformTarget("ModelLittleMaid_Aug");
            addTransformTarget("ModelLittleMaid_Aug", builtinModel);
            addTransformTarget("ModelLittleMaid_SR2");
            addTransformTarget("ModelLittleMaid_SR2", builtinModel);
            addTransformTarget("ModelLittleMaid_Orign");
            addTransformTarget("ModelLittleMaid_Orign", builtinModel);
            addTransformTarget("ModelLittleMaid_Archetype");
            addTransformTarget("ModelLittleMaid_Archetype", builtinModel);

            String model = "firis/lmmm/api/model/";
            addTransformTarget("ModelLittleMaidBase");
            addTransformTarget("ModelLittleMaidBase", model);
            addTransformTarget("ModelMultiMMMBase");
            addTransformTarget("ModelMultiMMMBase", model);
            addTransformTarget("ModelMultiBase");
            addTransformTarget("ModelMultiBase", model);
            addTransformTarget("ModelBase");
            addTransformTarget("ModelBase", model);

            put("net/blacklab/lmr/entity/EntityLittleMaid", "work/nemonet/littlemaidneo/entity/EntityLittleMaid");
            put("net/minecraft/entity/EntityLivingBase", "net/minecraft/entity/LivingEntity");
            put("net/minecraft/entity/passive/EntityAnimal", "net/minecraft/entity/passive/Animal");
            put("net/minecraft/entity/player/EntityPlayer", "net/minecraft/entity/player/Player");

            put("net/blacklab/lmr/entity/littlemaid/EntityLittleMaid", "work/nemonet/littlemaidneo/entity/EntityLittleMaid");
            if (FMLEnvironment.production) {
                put("net/minecraft/entity/Entity", "net/minecraft/world/entity/Entity");
            }
        }

        private void addTransformTarget(String className) {
            put("MMM_" + className, PACKAGE_STRING + className);
            put("mmmlibx/lib/multiModel/model/mc162/" + className, PACKAGE_STRING + className);
            put("net/blacklab/lmr/entity/maidmodel/" + className, PACKAGE_STRING + className);
        }

        private void addTransformTarget(String className, String oldPackage) {
            put(oldPackage + className, PACKAGE_STRING + className);
        }
    };

    private static final Set<String> GL_REPLACE_MODEL_RENDERER_SET = new ObjectOpenHashSet<>() {
        {
            add("glPushMatrix()V");
            add("glPopMatrix()V");
            add("glTranslatef(FFF)V");
            add("glScalef(FFF)V");
            add("glRotatef(FFFF)V");
            add("glColor3f(FFF)V");
            add("glMatrixMode(I)V");
            add("glGetFloat(ILjava/nio/FloatBuffer;)V");
            add("glLoadMatrix(Ljava/nio/FloatBuffer;)V");
            add("glMultMatrix(Ljava/nio/FloatBuffer;)V");
            add("glCallList(I)V");
            add("glEnable(I)V");
            add("glTexCoord2f(FF)V");
            add("glNormal3f(FFF)V");
            add("glVertex3f(FFF)V");
            add("glPushAttrib(I)V");
            add("glCullFace(I)V");
            add("glBegin(I)V");
            add("glEnd()V");
            add("glPopAttrib()V");
            add("glLoadIdentity()V");
            add("");
        }
    };

    private static final Set<String> GL_REPLACE_DUMMY_SET = new ObjectOpenHashSet<>() {
        {
            add("()V");
            add("(I)V");
            add("(FF)V");
            add("(FFF)V");
            add("(Ljava/nio/FloatBuffer;)V");
            add("(ILjava/nio/FloatBuffer;)V");
        }
    };

    public byte[] transform(byte[] basicClass) {
        ClassReader reader = new ClassReader(basicClass);
        ClassNode cNode = new ClassNode();
        reader.accept(cNode, 0);

        final AtomicBoolean changed = new AtomicBoolean(false);

        tryReplace(changed, cNode.superName, superName -> cNode.superName = superName);

        cNode.fields.parallelStream().forEach(fNode -> {
            tryReplace(changed, fNode.desc, desc -> fNode.desc = desc);
            tryReplace(changed, fNode.signature, signature -> fNode.signature = signature);
        });

        cNode.methods.parallelStream().forEach(mNode -> {
            tryReplace(changed, mNode.desc, desc -> mNode.desc = desc);

            if (mNode.localVariables != null) {
                mNode.localVariables.parallelStream().forEach(lNode -> {
                    if (lNode.desc != null) tryReplace(changed, lNode.desc, desc -> lNode.desc = desc);
                    if (lNode.name != null) tryReplace(changed, lNode.name, name -> lNode.name = name);
                    if (lNode.signature != null) tryReplace(changed, lNode.signature, sig -> lNode.signature = sig);
                });
            }

            AbstractInsnNode aNode = mNode.instructions.getFirst();
            while (aNode != null) {
                if (aNode instanceof FieldInsnNode fANode) {
                    tryReplace(changed, fANode.desc, desc -> fANode.desc = desc);
                    tryReplace(changed, fANode.name, name -> fANode.name = name);
                    tryReplace(changed, fANode.owner, owner -> fANode.owner = owner);
                } else if (aNode instanceof InvokeDynamicInsnNode fANode) {
                    tryReplace(changed, fANode.desc, desc -> fANode.desc = desc);
                    tryReplace(changed, fANode.name, name -> fANode.name = name);
                    for (int i = 0; i < fANode.bsmArgs.length; i++) {
                        var bsmArg = fANode.bsmArgs[i];
                        if (bsmArg instanceof Type type) {
                            int finalI = i;
                            if (type.getSort() == Type.METHOD) {
                                tryReplace(changed, type.getDescriptor(),
                                        desc -> fANode.bsmArgs[finalI] = Type.getMethodType(desc));
                            }
                        } else if (bsmArg instanceof Handle handle) {
                            int finalI = i;
                            tryReplace(changed, handle.getDesc(),
                                    desc -> fANode.bsmArgs[finalI] = new Handle(
                                            handle.getTag(), handle.getOwner(), handle.getName(),
                                            desc, handle.isInterface()));
                        }
                    }
                } else if (aNode instanceof MethodInsnNode fANode) {
                    if (shouldRemove(fANode.owner)) {
                        changed.set(true);
                        aNode = aNode.getNext();
                        if (GL_REPLACE_MODEL_RENDERER_SET.contains(fANode.name + fANode.desc)) {
                            mNode.instructions.set(fANode, new MethodInsnNode(fANode.getOpcode(),
                                    "work/nemonet/littlemaidneo/maidmodel/compat/GLCompat",
                                    fANode.name, fANode.desc));
                        } else if (GL_REPLACE_DUMMY_SET.contains(fANode.desc)) {
                            mNode.instructions.set(fANode, new MethodInsnNode(fANode.getOpcode(),
                                    "work/nemonet/littlemaidneo/maidmodel/compat/GLCompat",
                                    "dummy", fANode.desc));
                        } else {
                            mNode.instructions.set(fANode, new MethodInsnNode(fANode.getOpcode(),
                                    "work/nemonet/littlemaidneo/maidmodel/compat/GLCompat",
                                    "dummy", "()V"));
                        }
                        continue;
                    }
                    tryReplace(changed, fANode.desc, desc -> fANode.desc = desc);
                    tryReplace(changed, fANode.name, name -> fANode.name = name);
                    tryReplace(changed, fANode.owner, owner -> fANode.owner = owner);
                } else if (aNode instanceof MultiANewArrayInsnNode fANode) {
                    tryReplace(changed, fANode.desc, desc -> fANode.desc = desc);
                } else if (aNode instanceof TypeInsnNode fANode) {
                    tryReplace(changed, fANode.desc, desc -> fANode.desc = desc);
                } else if (aNode instanceof LdcInsnNode fANode && fANode.cst instanceof Type type) {
                    tryReplace(changed, type.getInternalName(),
                            desc -> fANode.cst = Type.getObjectType(desc));
                }
                aNode = aNode.getNext();
            }
        });

        if (changed.get()) {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cNode.accept(writer);
            return writer.toByteArray();
        }
        return basicClass;
    }

    private void tryReplace(AtomicBoolean changed, String text, Consumer<String> replacer) {
        String newText = null;
        for (Entry<String, String> entry : CODE_REPLACE_MAP.entrySet()) {
            if ((text + ";").contains(entry.getKey() + ";")) {
                text = text.replace(entry.getKey(), entry.getValue());
                newText = text;
            }
        }
        if (newText != null) {
            changed.set(true);
            replacer.accept(newText);
        }
    }

    private boolean shouldRemove(String text) {
        return text.endsWith("GL11");
    }
}
