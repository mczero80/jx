package jx.zero;

import jx.zero.debug.*;

public class CPUInfo {
    String vendor;
    int features;
    int type;
    int family;
    int model;
    int stepping;
    int brand;

    private static final int TYPE_PRIMARY_CPU       = 0;
    private static final int TYPE_OVERDRIVE_CPU     = 1;
    private static final int TYPE_SECONDARY_MP_CPU  = 2;
    private static final int TYPE_RESERVED          = 3;

    private static final int FAMILY_486  = 4;
    private static final int FAMILY_P5   = 5;
    private static final int FAMILY_P6   = 6;

    private static final int FEATURE_XMM_MASK  = 0x02000000;
    private static final int FEATURE_FXSR_MASK = 0x01000000;
    private static final int FEATURE_MMX_MASK  = 0x00800000;
    private static final int FEATURE_PSN_MASK  = 0x00040000;
    private static final int FEATURE_PSE36_MASK= 0x00020000;
    private static final int FEATURE_PAT_MASK  = 0x00010000;
    private static final int FEATURE_CMOV_MASK = 0x00008000;
    private static final int FEATURE_MCA_MASK  = 0x00004000;
    private static final int FEATURE_PGE_MASK  = 0x00002000;
    private static final int FEATURE_MTRR_MASK = 0x00001000;
    private static final int FEATURE_SEP_MASK  = 0x00000800;
    private static final int FEATURE_APIC_MASK = 0x00000200;
    private static final int FEATURE_CX8_MASK  = 0x00000100;
    private static final int FEATURE_MCE_MASK  = 0x00000080;
    private static final int FEATURE_PAE_MASK  = 0x00000040;
    private static final int FEATURE_MSR_MASK  = 0x00000020;
    private static final int FEATURE_TSC_MASK  = 0x00000010;
    private static final int FEATURE_PSE_MASK  = 0x00000008;
    private static final int FEATURE_DE_MASK   = 0x00000004;
    private static final int FEATURE_VME_MASK  = 0x00000002;
    private static final int FEATURE_FPU_MASK  = 0x00000001;

    public CPUInfo(String vendor) {
	this.vendor = vendor;
    }
    public CPUInfo(String vendor, int type, int family, int model, int stepping, int brand, int features) {
	this.vendor = vendor;
	this.type = type;
	this.family = family;
	this.model = model;
	this.stepping = stepping;
	this.brand = brand;
	this.features = features;
	/*
	  Debug.out.println("Vendor: "+vendor);
	Debug.out.println("Type: "+type);
	Debug.out.println("Family: "+family);
	Debug.out.println("Model: "+model);
	Debug.out.println("Stepping: "+stepping);
	Debug.out.println("Brand: "+brand);
	Debug.out.println("Features: 0x"+Integer.toHexString(features));
	*/
    }

    public boolean hasMTRR() { return (features & FEATURE_MTRR_MASK) != 0; }
}
