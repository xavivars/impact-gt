/*
 * Copyright (C) 2009
 *
 * Authors:
 *  Rafael C. Carrasco Jiménez
 *  Xavier Ivars i Ribes <xavi.ivars@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
package es.ua.impact.utils;

import es.ua.impact.utils.CmdOptions.Option;
import java.io.File;

public class CmdOptionTester {

    public String testDirectory(CmdOptions parser, Option opt,  boolean force, boolean read, boolean oblig) {
        String ret = (String) parser.getOptionValue(opt);

        if (ret == null) {
            if (oblig) {
                System.err.println("BAD USAGE. directory must be defined: {-f | --file} file");
                System.exit(0);
            } else {
                return null;
            }
        }

        if(ret.equalsIgnoreCase("-"))
            return ret;

        boolean cpok = true;

        File cpfile = new File(ret);
        if (!cpfile.exists()) {
            cpok = false;
        } else if (!cpfile.isDirectory()) {
            cpok = false;
        }
        
        if (!cpok) {
            System.err.println("BAD USAGE. file must be an existing file [" + ret + "]");
            System.exit(0);
        }
        
        return ret;
    }
    
    public String testFile(CmdOptions parser, Option opt,  boolean force, boolean read, boolean oblig) {
        String ret = (String) parser.getOptionValue(opt);

        if (ret == null) {
            if (oblig) {
                System.err.println("BAD USAGE. file must be defined: {-f | --file} file");
                System.exit(0);
            } else {
                return null;
            }
        }

        if(ret.equalsIgnoreCase("-"))
            return ret;

        boolean cpok = true;

        File cpfile = new File(ret);
        if (!cpfile.exists()) {
            cpok = false;
        } else if (!cpfile.isFile()) {
            cpok = false;
        }
        
        if (!cpok) {
            System.err.println("BAD USAGE. file must be an existing file [" + ret + "]");
            System.exit(0);
        }
        
        return ret;
    }

    public String testFile(CmdOptions parser, Option opt) {
        return testFile(parser,opt,false,false,false);
    }
    
    public String testDirectory(CmdOptions parser, Option opt) {
        return testDirectory(parser,opt,false,false,false);
    }
    
    public boolean testBoolean(CmdOptions parser, Option opt) {
        return (((Boolean) parser.getOptionValue(opt)) != null) ? true : false;
    }

}
