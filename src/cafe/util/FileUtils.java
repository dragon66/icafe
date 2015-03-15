/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FileUtils {
	
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
            			System.out.println("File " + file.getAbsolutePath() + " deleted");
            		} else {
            			System.out.println("File " + file.getAbsolutePath() + " was not deleted, unknown reason");
            		}
            	}
            }
        } else {
        	System.out.println("File " + file.getAbsolutePath() + " doesn't exist");
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
        	String path = dir.getAbsolutePath();        	
        	if(fileExt != null && path.endsWith(fileExt)) {
        		fileList.add(path);        	   
        		System.out.println(path);
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
	        @Override
	        public boolean accept(File file) {
	            return p.matcher(file.getName()).matches();
	        }
	    });
	}
	
	private FileUtils() {}
}