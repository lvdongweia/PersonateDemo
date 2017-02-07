/*#########################################################################################################
# Author: wuhaizhou,420660135@qq.com,15996211983                                                          #
#                                                                                                         #
# Data: 2016.11.15                                                                                        #
#########################################################################################################*/

package com.avatarmind.visionservice;

public interface OnDetectorListening {
    public void DetectorListening(String event,String lable, int PosX, int PoxY, int Width, int Height);
}
