/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * DCT.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    25Mar2014  Combined DCT and IDCT.
 */

package com.icafe4j.image.util;

/** 
 * DCT and IDCT transformation utility class used by JPEG encoders and decoders.
 * <p>
 * The current class is the AAN implementation which can be found at:
 * Y.Arai, T.Agui, M.Nakajima, A Fast DCT-SQ Scheme for Images, Trans. of the IEICE.E 71(11):1095(Nov.1988)
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/21/2013
 */
public class DCT {
	// Constants //Ck=cos(k*pi/16)
	private static final float BETA1 = 1.41421356f;//( 2*C4)
	private static final float BETA2 = 2.61312587f;//(2*(C2+C6))
	private static final float BETA3 = 1.41421356f;//(2*C4)
	private static final float BETA4 = 1.08239220f;//(2*(C2-C6))
	private static final float BETA5 = 0.76536686f;//(2*C6)
	
	/* AAN IDCT scale factor definition:
	 * AANscaleFactor[0] = 1
	 * AANscaleFactor[k] = cos(k*PI/16) * sqrt(2) for k=1..7
	 */
	// The scale factor is the same as those from the IJG's
	private static final  float[] AANscaleFactor = { 1.0f, 1.387039845f, 1.306562965f, 1.175875602f,
                                   1.0f, 0.785694958f, 0.541196100f, 0.275899379f};
	// Multiplier factors
	private static final float[][] MULTIPLIER = new float[8][8];
	
	static {
		for(int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
				MULTIPLIER[i][j] = 1.0f/(AANscaleFactor[i]*AANscaleFactor[j]*8.0f);
			}
		}
	}
	
	private DCT() { }

    /*
     * Fast DCT algorithm due to Arai, Agui, Nakajima
     */
    public static float[][] forwardDCT(float input[][]) {
        float tmp0, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7;
        float tmp10, tmp11, tmp12, tmp13;
        float z1, z2, z3, z4, z5, z11, z13;
        int i;
        /* transform rows */
        for (i = 0; i < 8; i++) {
        	/* Stage 1 */
            tmp0 = input[i][0] + input[i][7];
            tmp7 = input[i][0] - input[i][7];
            tmp1 = input[i][1] + input[i][6];
            tmp6 = input[i][1] - input[i][6];
            tmp2 = input[i][2] + input[i][5];
            tmp5 = input[i][2] - input[i][5];
            tmp3 = input[i][3] + input[i][4];
            tmp4 = input[i][3] - input[i][4];
        
            tmp10 = tmp0 + tmp3;
            tmp13 = tmp0 - tmp3;
            tmp11 = tmp1 + tmp2;
            tmp12 = tmp1 - tmp2;

            input[i][0] = tmp10 + tmp11;
            input[i][4] = tmp10 - tmp11;

            z1 = (tmp12 + tmp13) * 0.707106781f;
            input[i][2] = tmp13 + z1;
            input[i][6] = tmp13 - z1;

            tmp10 = tmp4 + tmp5;
            tmp11 = tmp5 + tmp6;
            tmp12 = tmp6 + tmp7;

            z5 = (tmp10 - tmp12) * 0.382683433f;
            z2 = 0.541196100f * tmp10 + z5;
            z4 = 1.306562965f * tmp12 + z5;
            z3 = tmp11 * 0.707106781f;

            z11 = tmp7 + z3;
            z13 = tmp7 - z3;

            input[i][5] = z13 + z2;
            input[i][3] = z13 - z2;
            input[i][1] = z11 + z4;
            input[i][7] = z11 - z4;
        }

        for (i = 0; i < 8; i++) {
            tmp0 = input[0][i] + input[7][i];
            tmp7 = input[0][i] - input[7][i];
            tmp1 = input[1][i] + input[6][i];
            tmp6 = input[1][i] - input[6][i];
            tmp2 = input[2][i] + input[5][i];
            tmp5 = input[2][i] - input[5][i];
            tmp3 = input[3][i] + input[4][i];
            tmp4 = input[3][i] - input[4][i];

            tmp10 = tmp0 + tmp3;
            tmp13 = tmp0 - tmp3;
            tmp11 = tmp1 + tmp2;
            tmp12 = tmp1 - tmp2;

            input[0][i] = (tmp10 + tmp11)*MULTIPLIER[0][i];
            input[4][i] = (tmp10 - tmp11)*MULTIPLIER[4][i];

            z1 = (tmp12 + tmp13) * 0.707106781f;
            input[2][i] = (tmp13 + z1)*MULTIPLIER[2][i];
            input[6][i] = (tmp13 - z1)*MULTIPLIER[6][i];

            tmp10 = tmp4 + tmp5;
            tmp11 = tmp5 + tmp6;
            tmp12 = tmp6 + tmp7;

            z5 = (tmp10 - tmp12) * 0.382683433f;
            z2 = 0.541196100f * tmp10 + z5;
            z4 = 1.306562965f * tmp12 + z5;
            z3 = tmp11 * 0.707106781f;

            z11 = tmp7 + z3;
            z13 = tmp7 - z3;

            input[5][i] = (z13 + z2)*MULTIPLIER[5][i];
            input[3][i] = (z13 - z2)*MULTIPLIER[3][i];
            input[1][i] = (z11 + z4)*MULTIPLIER[1][i];
            input[7][i] = (z11 - z4)*MULTIPLIER[7][i];
        }

        return input;
    }
    
    public static float[][] inverseDCT(float input[][])	{
		for (int i=0 ; i<8 ; i++) {
			inverseDCT_col(input, i*8);
		}
		
		for (int i=0 ; i<8 ; i++) {
			inverseDCT_row(input, i++);
		}
		
		return input;
	}
   
	//perform column transform
	private static void inverseDCT_col(float input[][], int offset)	{
		float tmp0, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7;
		float temp0, temp1, temp2, temp3, temp5;
		float temp;

		// Transform the even part of the input
		// Stages 1-4 
		
		tmp0 = input[0][offset] + input[4][offset];
		tmp1 = input[0][offset] - input[4][offset];
       
	    tmp3 = input[2][offset] + input[6][offset];//stage 1-4
	    tmp2 = (input[2][offset] - input[6][offset])*BETA1 - tmp3;//stage 1-4

		// Transform the odd part of the input
		// Stage 1
		tmp4 = input[5][offset] - input[3][offset];
		tmp5 = input[1][offset] + input[7][offset];
		tmp6 = input[1][offset] - input[7][offset];
		tmp7 = input[5][offset] + input[3][offset];
		// Stage 2
		temp5 = tmp5 - tmp7;
		tmp7 = tmp5 + tmp7;
		// Stages 3-4
		temp = (tmp4 - tmp6)*BETA5;
		tmp4 = - tmp4*BETA2 + temp;
		tmp6 = tmp6*BETA4 - temp;
		tmp5 = temp5*BETA3;
		// Stage 5, even part
		temp0 = tmp0 + tmp3;
		temp3 = tmp0 - tmp3;
		temp1 = tmp1 + tmp2;
		temp2 = tmp1 - tmp2;
		// Stage 5, odd part
		tmp6 = tmp6 - tmp7;
		tmp5 = tmp5 - tmp6;
		tmp4 = -(tmp4 + tmp5);
		// Stage 6, final stage
		input[0][offset] = temp0 + tmp7;
		input[7][offset] = temp0 - tmp7;
		input[1][offset] = temp1 + tmp6;
		input[6][offset] = temp1 - tmp6;
		input[2][offset] = temp2 + tmp5;
		input[5][offset] = temp2 - tmp5;
		input[3][offset] = temp3 + tmp4; 
		input[4][offset] = temp3 - tmp4;
	}
	
	// Perform row transform
	private static void inverseDCT_row(float input[][], int offset)	{
		float tmp0, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7;
		float temp0, temp1, temp2, temp3, temp5;
		float temp;

		// Transform the even part of the input
		// Stages 1-4 
		
		tmp0 = input[offset][0] + input[offset][4];
		tmp1 = input[offset][0] - input[offset][4];
       
	    tmp3 = input[offset][2] + input[offset][6];// Stages 1-4
	    tmp2 = (input[offset][2] - input[offset][6])*BETA1 - tmp3;// Stages 1-4

		// Transform the odd part of the input
		// Stage 1
		tmp4 = input[offset][5] - input[offset][3];
		tmp5 = input[offset][1] + input[offset][7];
		tmp6 = input[offset][1] - input[offset][7];
		tmp7 = input[offset][5] + input[offset][3];
		// Stage 2
		temp5 = tmp5 - tmp7;
		tmp7 = tmp5 + tmp7;
		// Stages 3-4
		temp = (tmp4 - tmp6)*BETA5;
		tmp4 = - tmp4*BETA2 + temp;
		tmp6 = tmp6*BETA4 - temp;
		tmp5 = temp5*BETA3;
		// Stage 5, even part
		temp0 = tmp0 + tmp3;
		temp3 = tmp0 - tmp3;
		temp1 = tmp1 + tmp2;
		temp2 = tmp1 - tmp2;
		// Stage 5, odd part
		tmp6 = tmp6 - tmp7;
		tmp5 = tmp5 - tmp6;
		tmp4 = -(tmp4 + tmp5);
		// Stage 6, final stage
		input[offset][0] = (temp0 + tmp7)*MULTIPLIER[offset][0];
		input[offset][7] = (temp0 - tmp7)*MULTIPLIER[offset][7];
		input[offset][1] = (temp1 + tmp6)*MULTIPLIER[offset][1];
		input[offset][6] = (temp1 - tmp6)*MULTIPLIER[offset][6];
		input[offset][2] = (temp2 + tmp5)*MULTIPLIER[offset][2];
		input[offset][5] = (temp2 - tmp5)*MULTIPLIER[offset][5];
		input[offset][3] = (temp3 + tmp4)*MULTIPLIER[offset][3]; 
		input[offset][4] = (temp3 - tmp4)*MULTIPLIER[offset][4];
	}
}