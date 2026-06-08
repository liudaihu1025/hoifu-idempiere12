package org.adempiere.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.report.MReportLine;

public class FormulaValidator {  
	private static final Pattern FORMULA_PATTERN = Pattern.compile("(?:-?\\d+(?:\\.\\d+)?|@\\d+|[+\\-*/()])+");
    private static String lastError = "";  
      
    public static boolean validateSyntax(String formula) {  
        if (formula == null || formula.trim().isEmpty()) {  
			lastError = "公式不能为空";
            return false;  
        }  
          
        // 基本语法检查  
        if (!validateBasicSyntax(formula)) {  
            return false;  
        }  
          
        // 括号匹配检查  
        if (!validateParentheses(formula)) {  
            return false;  
        }  
          
        // 行号引用检查  
        if (!validateRowReferences(formula)) {  
            return false;  
        }  
          
        return true;  
    }  
      
    private static boolean validateBasicSyntax(String formula) {  
		String noSpacesFormula = formula.replaceAll("\\s", "");
		Matcher matcher = FORMULA_PATTERN.matcher(noSpacesFormula);

		if (matcher.matches()) {
			return true;
		} else {
			lastError = "公式中包含无效字符";
			return false;
		}
    }  
      
    private static boolean validateParentheses(String formula) {  
        int balance = 0;  
        for (int i = 0; i < formula.length(); i++) {  
            char c = formula.charAt(i);  
            if (c == '(') {  
                balance++;  
            } else if (c == ')') {  
                balance--;  
                if (balance < 0) {  
					lastError = "在位置 " + i + " 处有未匹配的右括号";
                    return false;  
                }  
            }  
        }  
          
        if (balance != 0) {  
			lastError = "有未匹配的左括号";
            return false;  
        }  
          
        return true;  
    }  
      
    private static boolean validateRowReferences(String formula) {  
        Pattern rowPattern = Pattern.compile("@(\\d+)");  
        Matcher matcher = rowPattern.matcher(formula);  
          
        while (matcher.find()) {  
            try {  
                int rowId = Integer.parseInt(matcher.group(1));  
                if (rowId <= 0) {  
					lastError = "无效的行引用: @" + rowId;
                    return false;  
                }  
            } catch (NumberFormatException e) {  
				lastError = "无效的行号格式: " + matcher.group();
                return false;  
            }  
        }  
          
        return true;  
    }  

	public static boolean validateRuntime(String formula, MReportLine[] lines, int reportLineSetId) {
		if (!validateSyntax(formula)) {
			return false;
		}

		// 验证引用的行是否存在 - 在同一报表行集内按SeqNo查找
		Pattern rowPattern = Pattern.compile("@(\\d+)");
		Matcher matcher = rowPattern.matcher(formula);
		while (matcher.find()) {
			int referencedSeqNo = Integer.parseInt(matcher.group(1));
			boolean lineExists = false;

			for (MReportLine line : lines) {
				// 确保在同一个报表行集内查找
				if (line.getPA_ReportLineSet_ID() == reportLineSetId && line.getSeqNo() == referencedSeqNo) {
					lineExists = true;
					break;
				}
			}

			if (!lineExists) {
				lastError = "报表行集中未找到引用的行序号 " + referencedSeqNo;
				return false;
			}
		}
		return true;
	}
      
    public static String getLastError() {  
        return lastError;  
    }  
}