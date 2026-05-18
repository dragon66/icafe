/**
 * COPYRIGHT (C) 2014-2019 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
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
	// Constants for AAN DCT/IDCT algorithm
	private static final float BETA1 = 1.41421356f;//( 2*C4) = sqrt(2)
	private static final float BETA2 = 2.61312587f;//(2*(C2+C6))
	private static final float BETA3 = 1.84775907f;//(2*C4) - used in IDCT
	private static final float BETA4 = 1.08239220f;//(2*(C2-C6))
	
	/* AAN IDCT scale factor definition:
	 * AANscaleFactor[0] = 1
	 * AANscaleFactor[k] = cos(k*PI/16) * sqrt(2) for k=1..7
	 */
	// The scale factor is the same as those from the IJG's
	private static final  float[] AANscaleFactor = { 1.0f, 1.387039845f, 1.306562965f, 1.175875602f,
                                   1.0f, 0.785694958f, 0.541196100f, 0.275899379f};
	// Multiplier factors for DCT scaling: 1/(AANscaleFactor[i]*AANscaleFactor[j]*8)
	// Forward DCT applies these multipliers in column pass to normalize output
	// Inverse DCT undoes this by multiplying by AANscaleFactor and dividing by 8
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
		for (int i = 0 ; i < 8 ; i++) {
			inverseDCT_col(input, i);
		}
		
		for (int i = 0 ; i < 8 ; i++) {
			inverseDCT_row(input, i);
		}
		
		return input;
	}
   
	// Perform column transform (Pass 1)
	// Implements AAN IDCT algorithm (Arai, Agui, Nakajima)
	private static void inverseDCT_col(float input[][], int col) {
		// Check for DC-only column (all AC terms zero) - optimization
		if (input[1][col] == 0 && input[2][col] == 0 && input[3][col] == 0 &&
			input[4][col] == 0 && input[5][col] == 0 && input[6][col] == 0 &&
			input[7][col] == 0) {
				// AC terms all zero - just replicate DC value
				float dcval = input[0][col];
				input[0][col] = dcval;
				input[1][col] = dcval;
				input[2][col] = dcval;
				input[3][col] = dcval;
				input[4][col] = dcval;
				input[5][col] = dcval;
				input[6][col] = dcval;
				input[7][col] = dcval;
				return;
			}

		float tmp0, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7;
		float tmp10, tmp11, tmp12, tmp13;
		float z5, z10, z11, z12, z13;

		// Pre-scale to undo forward DCT normalization (multiply by AANscaleFactor[row])
		// Even part
		tmp0 = input[0][col] * AANscaleFactor[0];
		tmp1 = input[2][col] * AANscaleFactor[2];
		tmp2 = input[4][col] * AANscaleFactor[4];
		tmp3 = input[6][col] * AANscaleFactor[6];

		tmp10 = tmp0 + tmp2;
		tmp11 = tmp0 - tmp2;

		tmp13 = tmp1 + tmp3;
		tmp12 = (tmp1 - tmp3) * BETA1 - tmp13;

		tmp0 = tmp10 + tmp13;
		tmp3 = tmp10 - tmp13;
		tmp1 = tmp11 + tmp12;
		tmp2 = tmp11 - tmp12;

		// Odd part
		tmp4 = input[1][col] * AANscaleFactor[1];
		tmp5 = input[3][col] * AANscaleFactor[3];
		tmp6 = input[5][col] * AANscaleFactor[5];
		tmp7 = input[7][col] * AANscaleFactor[7];

		z13 = tmp6 + tmp5;
		z10 = tmp6 - tmp5;
		z11 = tmp4 + tmp7;
		z12 = tmp4 - tmp7;

		tmp7 = z11 + z13;
		tmp11 = (z11 - z13) * BETA1;

		z5 = (z10 + z12) * BETA3;
		tmp10 = z12 * BETA4 - z5;
		tmp12 = z10 * (-BETA2) + z5;

		tmp6 = tmp12 - tmp7;
		tmp5 = tmp11 - tmp6;
		tmp4 = tmp10 + tmp5;

		// Final output stage
		input[0][col] = tmp0 + tmp7;
		input[7][col] = tmp0 - tmp7;
		input[1][col] = tmp1 + tmp6;
		input[6][col] = tmp1 - tmp6;
		input[2][col] = tmp2 + tmp5;
		input[5][col] = tmp2 - tmp5;
		input[4][col] = tmp3 + tmp4;
		input[3][col] = tmp3 - tmp4;
	}

	// Perform row transform (Pass 2)
	// Implements AAN IDCT algorithm (Arai, Agui, Nakajima)
	private static void inverseDCT_row(float input[][], int row){
		// Check for DC-only row (all AC terms zero) - optimization
		if (input[row][1] == 0 && input[row][2] == 0 && input[row][3] == 0 &&
			input[row][4] == 0 && input[row][5] == 0 && input[row][6] == 0 &&
			input[row][7] == 0) {
			// AC terms all zero - just replicate DC value (divide by 8 to undo forward DCT scaling)
			float dcval = input[row][0] / 8.0f;
			input[row][0] = dcval;
			input[row][1] = dcval;
			input[row][2] = dcval;
			input[row][3] = dcval;
			input[row][4] = dcval;
			input[row][5] = dcval;
			input[row][6] = dcval;
			input[row][7] = dcval;
			return;
		}

		float tmp0, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7;
		float tmp10, tmp11, tmp12, tmp13;
		float z5, z10, z11, z12, z13;

		// Pre-scale to undo forward DCT normalization (multiply by AANscaleFactor[col])
		// Even part
		tmp10 = input[row][0] * AANscaleFactor[0] + input[row][4] * AANscaleFactor[4];
		tmp11 = input[row][0] * AANscaleFactor[0] - input[row][4] * AANscaleFactor[4];
		
		tmp13 = input[row][2] * AANscaleFactor[2] + input[row][6] * AANscaleFactor[6];
		tmp12 = (input[row][2] * AANscaleFactor[2] - input[row][6] * AANscaleFactor[6]) * BETA1 - tmp13;

		tmp0 = tmp10 + tmp13;
		tmp3 = tmp10 - tmp13;
		tmp1 = tmp11 + tmp12;
		tmp2 = tmp11 - tmp12;

		// Odd part
		z13 = input[row][5] * AANscaleFactor[5] + input[row][3] * AANscaleFactor[3];
		z10 = input[row][5] * AANscaleFactor[5] - input[row][3] * AANscaleFactor[3];
		z11 = input[row][1] * AANscaleFactor[1] + input[row][7] * AANscaleFactor[7];
		z12 = input[row][1] * AANscaleFactor[1] - input[row][7] * AANscaleFactor[7];

		tmp7 = z11 + z13;
		tmp11 = (z11 - z13) * BETA1;

		z5 = (z10 + z12) * BETA3;
		tmp10 = z12 * BETA4 - z5;
		tmp12 = z10 * (-BETA2) + z5;

		tmp6 = tmp12 - tmp7;
		tmp5 = tmp11 - tmp6;
		tmp4 = tmp10 + tmp5;

		// Final output stage (divide all by 8 to undo forward DCT scaling)
		input[row][0] = (tmp0 + tmp7) / 8.0f;
		input[row][7] = (tmp0 - tmp7) / 8.0f;
		input[row][1] = (tmp1 + tmp6) / 8.0f;
		input[row][6] = (tmp1 - tmp6) / 8.0f;
		input[row][2] = (tmp2 + tmp5) / 8.0f;
		input[row][5] = (tmp2 - tmp5) / 8.0f;
		input[row][4] = (tmp3 + tmp4) / 8.0f;
		input[row][3] = (tmp3 - tmp4) / 8.0f;
	}
}
