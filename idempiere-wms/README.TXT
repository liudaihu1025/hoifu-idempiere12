/******************************************************************************
 * Product: iDempiere Free ERP Project based on Compiere (2006)               *
 * Copyright (C) 2014 Redhuan D. Oon All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *  FOR NON-COMMERCIAL DEVELOPER USE ONLY                                     *
 *  @author Redhuan D. Oon  - red1@red1.org  www.red1.org                     *
 *****************************************************************************/

README FOR IDEMPIERE OSGI SAMPLE PLUGIN
PLEASE NOTE THAT THIS IS FOR NON COMMERCIAL DEVELOPER USE
FREE SOFTWARE IS FREE AS IN FREEDOM NOT FREE LUNCH

THIS SAMPLE IS FOR EASE OF CREATING A NEW PLUGIN INTO THE IDEMPIERE ERP SUITE. IT HAS:
1. MINIMAL PLUGIN FOR YOU TO CONTINUE FROM
2. MANIFEST FILE CONTAINING ACTIVATOR AND BASIC DEPENDENCY SETTING
3. OSGI-INF XMLS FOR MODELVALIDATOR OR DOCEVENT HANDLING AND MODELFACTORY DEFINITION
4. XMLS SET TO PluginDocEvent.java and PluginModelValidator.java
5. CALLOUT EXTENSION STUB
6. PROCESS EXTENSION STUB
7. CORE MODEL EXTENSION (SAMPLE MORDER.NEWFIELD - AMEND OR DISCARD FROM DOCEVENT)
8. POWRAPPER STUB WITH SETTER/GETTER OF EXTENDED PROPERTY TO CORE MODEL.

EXPORT YOUR DB MODEL CHANGE AS PACKOUT, RENAME TO 2Pack.zip AND PLACE UNDER META-INF FOLDER