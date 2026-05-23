package work.nemonet.littlemaidneo.maidmodel;

import java.util.Map;

public interface IModelCaps {

    int caps_onGround = 0x0001;
    int caps_isRiding = 0x0002;
    int caps_isChild = 0x0003;
    int caps_isUpdateSize = 0x0004;

    int caps_heldItemLeft = 0x0010;
    int caps_heldItemRight = 0x0011;
    int caps_heldItems = 0x0012;
    int caps_isSneak = 0x0013;
    int caps_aimedBow = 0x0014;

    int caps_Entity = 0x0020;
    int caps_health = 0x0021;
    int caps_ticksExisted = 0x0022;
    int caps_currentEquippedItem = 0x0023;
    int caps_currentArmor = 0x0024;
    int caps_healthFloat = 0x0025;
    int caps_TextureEntity = 0x0026;
    int caps_currentLeftHandItem = 0x0027;
    int caps_currentRightHandItem = 0x0028;

    int caps_isWet = 0x0030;
    int caps_isDead = 0x0031;
    int caps_isJumping = 0x0032;
    int caps_isInWeb = 0x0033;
    int caps_isSwingInProgress = 0x0034;
    int caps_isBurning = 0x0036;
    int caps_isInWater = 0x0037;
    int caps_isInvisible = 0x0038;
    int caps_isSprinting = 0x0039;
    int caps_isLeeding = 0x003a;
    int caps_getRidingName = 0x003b;
    int caps_getRidingType = 0x003c;
    int caps_entityName = 0x003d;

    int caps_posX = 0x0060;
    int caps_posY = 0x0061;
    int caps_posZ = 0x0062;
    int caps_pos = 0x0063;
    int caps_motionX = 0x0064;
    int caps_motionY = 0x0065;
    int caps_motionZ = 0x0066;
    int caps_motion = 0x0067;
    int caps_boundingBox = 0x0068;
    int caps_rotationYaw = 0x0069;
    int caps_rotationPitch = 0x006a;
    int caps_prevRotationYaw = 0x006b;
    int caps_prevRotationPitch = 0x006c;
    int caps_renderYawOffset = 0x006d;
    int caps_renderRidingYOffset = 0x006e;

    int caps_PosBlockID = 0x0081;
    int caps_PosBlockState = 0x0082;
    int caps_PosBlockAir = 0x0083;
    int caps_PosBlockLight = 0x0084;
    int caps_PosBlockPower = 0x0085;
    int caps_isRidingPlayer = 0x0086;

    int caps_WorldTotalTime = 0xff00;
    int caps_WorldTime = 0xff01;
    int caps_MoonPhase = 0xff02;

    int caps_isRendering = 0x0100;
    int caps_isBloodsuck = 0x0101;
    int caps_isFreedom = 0x0102;
    int caps_isTracer = 0x0103;
    int caps_isPlaying = 0x0104;
    int caps_isLookSuger = 0x0105;
    int caps_isBlocking = 0x0106;
    int caps_isWait = 0x0107;
    int caps_isWaitEX = 0x0108;
    int caps_isOpenInv = 0x0109;
    int caps_isWorking = 0x010a;
    int caps_isWorkingDelay = 0x010b;
    int caps_isContract = 0x010c;
    int caps_isContractEX = 0x010d;
    int caps_isRemainsC = 0x010e;
    int caps_isClock = 0x010f;
    int caps_isMasked = 0x0110;
    int caps_isCamouflage = 0x0111;
    int caps_isPlanter = 0x0112;
    int caps_isOverdrive = 0x0113;
    int caps_isOverdriveDelay = 0x0114;

    int caps_entityIdFactor = 0x0120;
    int caps_height = 0x0121;
    int caps_width = 0x0122;
    int caps_YOffset = 0x0123;
    int caps_mountedYOffset = 0x0124;
    int caps_dominantArm = 0x0125;
    int caps_render = 0x0130;
    int caps_Arms = 0x0131;

    @Deprecated
    int caps_HeadMount = 0x0132;
    int caps_HardPoint = 0x0133;
    int caps_stabiliser = 0x0134;
    int caps_Items = 0x0135;
    int caps_Actions = 0x0136;
    int caps_Grounds = 0x0137;
    int caps_Inventory = 0x0138;
    int caps_Ground = 0x0139;
    int caps_interestedAngle = 0x0150;
    int caps_job = 0x0151;

    int caps_ScaleFactor = 0x0200;
    int caps_PartsVisible = 0x0201;
    int caps_Posing = 0x0202;
    int caps_Actors = 0x0203;
    int caps_PartsStrings = 0x0204;

    int caps_changeModel = 0x0300;
    int caps_renderFace = 0x0310;
    int caps_renderBody = 0x0311;
    int caps_setFaceTexture = 0x0312;
    int caps_textureData = 0x0313;
    int caps_textureLightColor = 0x0314;

    int caps_motionSitting = 0x0401;

    int caps_isSwimming = 0x0600;
    int caps_roll = 0x0601;
    int caps_leaningPitch = 0x0602;
    int caps_lastLeaningPitch = 0x0603;
    int caps_isUsingRiptide = 0x0604;
    int caps_isFallFlying = 0x0605;

    int caps_pose = 0x0700;
    int caps_isPoseStanding = 0x0701;
    int caps_isPoseFallFlying = 0x0702;
    int caps_isPoseSleeping = 0x0703;
    int caps_isPoseSwimming = 0x0704;
    int caps_isPoseSpinAttack = 0x0705;
    int caps_isPoseCrouching = 0x0706;
    int caps_isPoseDying = 0x0707;

    int caps_sleepingDirection = 0x0800;

    Map<String, Integer> getModelCaps();

    Object getCapsValue(int pIndex, Object... pArg);

    @Deprecated
    boolean setCapsValue(int pIndex, Object... pArg);
}
