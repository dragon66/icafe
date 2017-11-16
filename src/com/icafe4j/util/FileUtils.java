/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);
		
	public static void delete(String fileName) {
		delete(fileName, "");
	}
	
	public static void delete(String fileName, String fileExt) {
		 File file = new File(fileName);
		 delete(file, fileExt);
	}
	
	public static String getNameWithoutExtension(File file) {
		return file.getName().replaceFirst("[.][^.]+$", "");
	}
	
	// Delete the file or if it's a directory, all files in the directory
	public static void delete(File file, final String fileExt) {
        if (file.exists()) {
            //check if the file is a directory
            if (file.isDirectory()) {            	
            	File[] files = file.listFiles();
            	for(File f:files){
            		//call deletion of file individually
            		delete(f, fileExt);
            	}                
            } else {
            	String path = file.getAbsolutePath();
            	if(fileExt != null && path.endsWith(fileExt)) {
            		boolean result = file.delete();
            		// test if delete of file is success or not
            		if (result) {
            			LOGGER.info("File {} deleted", file.getAbsolutePath());
            		} else {
            			LOGGER.info("File {} was not deleted, unknown reason", file.getAbsolutePath());
            		}
            	}
            }
        } else {
        	LOGGER.info("File {} doesn't exist", file.getAbsolutePath());
        }
    }
	
	public static List<String> list(String dir) {
		return list(dir, "");
	}
	
	// List all files in a directory
	public static List<String> list(String dir, String fileExt) {
		 File file = new File(dir);
		return list(file, fileExt);
    }
	
	// List all files in a directory with the specified extension
	public static List<String> list(File dir, final String fileExt) {
		//
		List<String> fileList = new ArrayList<String>();		
            
        //For all files and folders in directory
        if(dir.isDirectory()) {
        	//Get a list of all files and folders in directory
        	File[] files = dir.listFiles();
        	for(File f:files) {
	        	//Recursively call file list function on the new directory
	        	list(f, fileExt);
	        }        	
        }  else {
        	//If not directory, print the file path and add it to return list
        	String path = "";
			try {
				path = dir.getCanonicalPath();
			} catch (IOException e) {
				LOGGER.error("IOException", e);
			}        	
        	if(fileExt != null && path.endsWith(fileExt)) {
        		fileList.add(path);        	   
        		LOGGER.info("File: {}", path);
        	}
        }
        
        return fileList;
    }
	
	// From: stackoverflow.com
	public static File[] listFilesMatching(File root, String regex) {
	    if(!root.isDirectory()) {
	        throw new IllegalArgumentException(root+" is not a directory.");
	    }
	    final Pattern p = Pattern.compile(regex);
	    return root.listFiles(new FileFilter(){
	        public boolean accept(File file) {
	            return p.matcher(file.getName()).matches();
	        }
	    });
	}
	
	private FileUtils() {}
}