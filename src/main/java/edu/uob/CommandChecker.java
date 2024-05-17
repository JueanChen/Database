package edu.uob;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CommandChecker {
    public String checkCmdResult;
    public String filePath;
    public String[] cmdArr;
    public int cmdArrPointer;
    public StringBuffer checkCommandMessage = new StringBuffer();
    public String[] invalidNameList = {
            "USE", "CREATE", "DATABASE", "TABLE", "DROP", "ALTER", "INSERT",
            "INTO", "VALUES", "SELECT", "FROM", "WHERE", "UPDATE", "SET", "DELETE",
            "JOIN", "AND", "ON", "ADD", "TRUE", "FALSE", "OR", "LIKE"
    };
    public char[] symbolList = {'!', '#', '$', '%', '&', '(', ')', '*', '+', ',', '-',
                                '.', '/', ':', ';', '>', '=', '<', '?', '@', '[', '\\',
                                ']', '^', '_', '`', '{', '}', '~'};
    public List<String> attributeList = new ArrayList<>();
    public List<String> condition = new ArrayList<>();
    CommandExecutor executor = new CommandExecutor();
    public CommandChecker() {
        checkCmdResult = "[OK]\n";
        cmdArrPointer = 0;
    }
    public String checkCommand(String command) {
        String result;
        checkCmdResult = "[OK]\n";
        cmdArrPointer = 0;
        condition = new ArrayList<>();
        checkCommandMessage = new StringBuffer();
        attributeList = new ArrayList<>();
        List<String> cmd = new ArrayList<>();
        command = command.trim();
        StringBuffer temp = new StringBuffer();
        for (int i = 0; i < command.length(); i++) {
            char ch = command.charAt(i);
            if (ch != ' ' && ch != ';' && ch != ',' && ch != '\'') {
                if (ch == '<' || ch == '>' || ch == '=' || ch == '!') {
                    if (temp.length() != 0) {
                        cmd.add(temp.toString());
                        temp = new StringBuffer();
                    }
                    temp.append(ch);
                    char chNext = command.charAt(i+1);
                    if (chNext == '=') {
                        temp.append(chNext);
                        i++;
                    }
                    cmd.add(temp.toString());
                    temp = new StringBuffer();
                }
                else {
                    temp.append(ch);
                }
            }
            else if (ch == ';') {
                temp.append(ch);
                cmd.add(temp.toString());
                temp = new StringBuffer();
            }
            else if (ch == ',') {
                temp.append(ch);
                cmd.add(temp.toString());
                temp = new StringBuffer();
            }
            else if (ch == '\'') {
                temp.append(ch);
                i++;
                for (i = i; command.charAt(i) != '\''; i++) {
                    ch = command.charAt(i);
                    temp.append(ch);
                }
                ch = command.charAt(i);
                temp.append(ch);
            }
            else {
                if (temp.length() != 0) {
                    cmd.add(temp.toString());
                    temp = new StringBuffer();
                }
            }
        }
        if (temp.length() != 0) {
            cmd.add(temp.toString());
        }
        cmdArr = cmd.toArray(new String[cmd.size()]);
//        for (int i = 0; i < cmdArr.length; i++) {
//            System.out.println("no: " + i + " is " + cmdArr[i]);
//        }
        if (!cmdArr[cmdArr.length-1].endsWith(";")) {
            checkCmdResult = "[ERROR]:";
            checkCommandMessage.append("Semi colon missing at end of line (or similar message !)\n");
        }
        else {
            cmdArr[cmdArr.length-1] = removeLastChar(cmdArr[cmdArr.length-1]);
        }
        checkCommandType();
        if (checkCmdResult.equals("[OK]\n")) {
            if (cmdArr[0].equalsIgnoreCase("SELECT") ||
                cmdArr[0].equalsIgnoreCase("JOIN")) {
                checkCmdResult = executor.printResult();
            }
        }
        result = checkCmdResult + checkCommandMessage.toString();
        return result;
    }

    public void checkCommandType() {
        if (cmdArr[cmdArrPointer].equalsIgnoreCase("USE")) {
            checkUse();
        }
        else if (cmdArr[cmdArrPointer].equalsIgnoreCase("CREATE")) {
            checkCreate();
        }
        else if (cmdArr[cmdArrPointer].equalsIgnoreCase("DROP")) {
            checkDrop();
        }
        else if (cmdArr[cmdArrPointer].equalsIgnoreCase("ALTER")) {
            checkAlter();
        }
        else if (cmdArr[cmdArrPointer].equalsIgnoreCase("INSERT")) {
            checkInsert();
        }
        else if (cmdArr[cmdArrPointer].equalsIgnoreCase("SELECT")) {
            checkSelect();
        }
        else if (cmdArr[cmdArrPointer].equalsIgnoreCase("UPDATE")) {
            checkUpdate();
        }
        else if (cmdArr[cmdArrPointer].equalsIgnoreCase("DELETE")) {
            checkDelete();
        }
        else if (cmdArr[cmdArrPointer].equalsIgnoreCase("JOIN")) {
            checkJoin();
        }
        else {
            addErrorMessage("Command Type", "the");
        }
    }

    public void checkUse() {
        pointerIncrease();
        checkDatabaseName();
        if (checkCmdResult.equals("[OK]\n")) {
            executor.use(cmdArr);
        }
    }

    public void checkCreate() {
        pointerIncrease();
        if (cmdArr[cmdArrPointer].equalsIgnoreCase("DATABASE")) {
            pointerIncrease();
            checkDatabaseName();
        }
        else if (cmdArr[cmdArrPointer].equalsIgnoreCase("TABLE")) {
            pointerIncrease();
            checkTableName();
            if (cmdArrPointer < cmdArr.length-1) {
                pointerIncrease();
                checkLeftBracket();
                checkRightBracket();
                checkAttributeList();
            }
        }
        else {
            addErrorMessage("'DATABASE/TABLE'", "CREATE");
        }
        if (checkCmdResult.equals("[OK]\n")) {
            executor.create(cmdArr);
        }
    }

    public void checkDrop() {
        pointerIncrease();
        if (cmdArr[cmdArrPointer].equalsIgnoreCase("DATABASE")) {
            pointerIncrease();
            checkDatabaseName();
        }
        else if (cmdArr[cmdArrPointer].equalsIgnoreCase("TABLE")) {
            pointerIncrease();
            checkTableName();
        }
        else {
            addErrorMessage("'DATABASE/TABLE'", "DROP");
        }
        if (checkCmdResult.equals("[OK]\n")) {
            executor.drop(cmdArr);
        }
    }

    public void checkAlter() {
        if (checkStringEquals("TABLE", "ALTER")) {
            checkTableName();
            checkAlterationType();
            if (cmdArr[cmdArrPointer].equalsIgnoreCase("id")) {
                addErrorMessage("Alter's attribute", "ALTER");
            }
            checkAttributeName();
        }
        if (checkCmdResult.equals("[OK]\n")) {
            executor.alter(cmdArr);
        }
    }

    public void checkInsert() {
        if (checkStringEquals("INTO", "INSERT")) {
            checkTableName();
            if (checkStringEquals("VALUES", "INSERT")) {
                checkLeftBracket();
                checkRightBracket();
                checkValueList();
            }
        }
        if (checkCmdResult.equals("[OK]\n")) {
            checkInsertNum();
        }
        if (checkCmdResult.equals("[OK]\n")) {
            executor.insert(cmdArr);
        }
    }

    public void checkInsertNum() {
        int insertNum = cmdArr.length - 4;
        int tableColNum = 0;
        String filePath = executor.getFilePath("TABLE", cmdArr[2]);
        File table = new File(filePath);
        if (table.length() != 0) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
                String firstLine = bufferedReader.readLine();
                for (int i = 0; i < firstLine.length(); i++) {
                    if (firstLine.charAt(i) == '\t') {
                        tableColNum++;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (insertNum != tableColNum) {
                addErrorMessage("VALUE's number", "INSERT");
            }
        }
    }

    public void checkSelect() {
        int tableIndex = 0;
        while (!cmdArr[tableIndex].equalsIgnoreCase("FROM")) {
            tableIndex++;
        }
        tableIndex++;
        filePath = executor.getFilePath("TABLE", cmdArr[tableIndex]);
        checkWildAttribList();
        if (checkStringEquals("FROM", "SELECT")) {
            checkTableName();
            if (cmdArrPointer < cmdArr.length-1) {
                checkCondition1("SELECT");
            }
        }
        if (checkCmdResult.equals("[OK]\n")) {
            executor.select(cmdArr, tableIndex, condition);
        }
    }

    public void checkUpdate() {
        pointerIncrease();
        checkTableName();
        if (checkStringEquals("SET", "UPDATE")) {
            checkNameValueList();
            checkCondition1("UPDATE");
        }
        if (checkCmdResult.equals("[OK]\n")) {
            executor.update(cmdArr, condition);
        }
    }

    public void checkDelete() {
        if (checkStringEquals("FROM", "DELETE")) {
            checkTableName();
            checkCondition1("DELETE");
        }
        if (checkCmdResult.equals("[OK]\n")) {
            executor.delete(cmdArr, condition);
        }
    }

    public void checkJoin() {
        pointerIncrease();
        checkTableName();
        if (checkStringEquals("AND", "JOIN")) {
            checkTableName();
            if (checkStringEquals("ON", "JOIN")) {
                checkAttributeName();
                if (checkStringEquals("AND", "JOIN")) {
                    checkAttributeName();
                }
            }
        }
        if (checkCmdResult.equals("[OK]\n")) {
            executor.join(cmdArr);
        }
    }

    public boolean checkPlainText() {
        char ch;
        for (int i = 0; i < cmdArr[cmdArrPointer].length(); i++) {
            ch = cmdArr[cmdArrPointer].charAt(i);
            if (!Character.isDigit(ch) && !Character.isLetter(ch) && ch != ' ') {
                return false;
            }
        }
        return true;
    }

    public boolean checkSymbol(char ch) {
        for (char c : symbolList) {
            if (ch == c) {
                return true;
            }
        }
        return false;
    }

    public void checkNameValueList() {
        for (int i = 3; !cmdArr[i].equalsIgnoreCase("WHERE"); i++) {
            if (cmdArr[cmdArrPointer].endsWith(",")) {
                cmdArr[cmdArrPointer] = removeLastChar(cmdArr[cmdArrPointer]);
            }
        }
        checkNameValuePair();
        if (!cmdArr[cmdArrPointer].equalsIgnoreCase("WHERE")) {
            checkNameValueList();
        }
        else {
            cmdArrPointer--;
        }
    }

    public void checkNameValuePair() {
        checkAttributeName();
        if (checkStringEquals("=", "NameValuePair")) {
            checkValue();
        }
        pointerIncrease();
    }

    public void checkAlterationType() {
        pointerIncrease();
        if (cmdArr[cmdArrPointer].equalsIgnoreCase("ADD")) {
            pointerIncrease();
        }
        else if (cmdArr[cmdArrPointer].equalsIgnoreCase("DROP")) {
            pointerIncrease();
        }
        else {
            addErrorMessage("Alteration Type (ADD/DROP)", cmdArr[0]);
        }
    }

    public void checkValueList() {
        if (cmdArr[cmdArrPointer].endsWith(",")) {
            cmdArr[cmdArrPointer] = removeLastChar(cmdArr[cmdArrPointer]);
            checkValue();
            pointerIncrease();
            checkValueList();
        }
        else {
            checkValue();
        }
    }

    public boolean checkDigitSequence(int start, int end) {
        char ch;
        for (int i = start; i < end; i++) {
            ch = cmdArr[cmdArrPointer].charAt(i);
            if (!Character.isDigit(ch)) {
                return false;
            }
        }
        return true;
    }

    public boolean checkIntegerLiteral() {
        char sign = cmdArr[cmdArrPointer].charAt(0);
        if (sign == '-' || sign == '+') {
            return checkDigitSequence(1, cmdArr[cmdArrPointer].length());
        }
        else {
            return checkDigitSequence(0, cmdArr[cmdArrPointer].length());
        }
    }

    public boolean checkFloatLiteral() {
        int decimalPointPos = 0;
        int decimalPointCnt = 0;
        char sign = cmdArr[cmdArrPointer].charAt(0);
        char ch;
        for (int i = 0; i < cmdArr[cmdArrPointer].length(); i++) {
            ch = cmdArr[cmdArrPointer].charAt(i);
            if (ch == '.') {
                decimalPointPos = i;
                decimalPointCnt++;
            }
        }
        if (decimalPointCnt == 0 || decimalPointCnt > 1) {
            return false;
        }
        if (sign == '-' || sign == '+') {
            return (checkDigitSequence(1, decimalPointPos) &&
                    checkDigitSequence(decimalPointPos+1, cmdArr[cmdArrPointer].length()));
        }
        else {
            return (checkDigitSequence(0, decimalPointPos) &&
                    checkDigitSequence(decimalPointPos+1, cmdArr[cmdArrPointer].length()));
        }
    }

    public boolean checkBooleanLiteral() {
        return (cmdArr[cmdArrPointer].equalsIgnoreCase("TRUE") ||
                cmdArr[cmdArrPointer].equalsIgnoreCase("FALSE"));
    }

    public boolean checkStringLiteral() {
        char ch;
        if (cmdArr[cmdArrPointer].startsWith("'") && cmdArr[cmdArrPointer].endsWith("'")) {
            cmdArr[cmdArrPointer] = removeFirstChar(cmdArr[cmdArrPointer]);
            cmdArr[cmdArrPointer] = removeLastChar(cmdArr[cmdArrPointer]);
            checkName(cmdArr[cmdArrPointer]);
            if (cmdArr[cmdArrPointer].equalsIgnoreCase("id")) {
                addErrorMessage("Value", "the");
            }
            for (int i = 0; i < cmdArr[cmdArrPointer].length(); i++) {
                ch = cmdArr[cmdArrPointer].charAt(i);
                if (!Character.isDigit(ch) && !Character.isLetter(ch) && !checkSymbol(ch) && ch != ' ') {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }

    public void checkValue() {
        if (cmdArr[cmdArrPointer].length() == 0) {
            addErrorMessage("Value", "the");
        }
        else {
            if (checkStringLiteral()) {
                return;
            } else if (checkBooleanLiteral()) {
                return;
            } else if (checkFloatLiteral()) {
                return;
            } else if (checkIntegerLiteral()) {
                return;
            } else if (cmdArr[cmdArrPointer].equals("NULL")) {
                return;
            } else {
                addErrorMessage("Value", cmdArr[0]);
                return;
            }
        }
    }

    public void checkTableName() {
        checkName(cmdArr[cmdArrPointer]);
        compareName("TABLE");
        if (!checkPlainText()) {
            addErrorMessage("Table name", cmdArr[0]);
        }
    }

    public void checkDatabaseName() {
        checkName(cmdArr[cmdArrPointer]);
        compareName("DATABASE");
        if (!checkPlainText()) {
            addErrorMessage("Database name", cmdArr[0]);
        }
    }

    public void checkWildAttribList() {
        pointerIncrease();
        if (!cmdArr[cmdArrPointer].equals("*")) {
            checkAttributeList();
        }
    }

    public void checkAttributeList() {
        if (cmdArr[cmdArrPointer].endsWith(",")) {
            cmdArr[cmdArrPointer] = removeLastChar(cmdArr[cmdArrPointer]);
            checkAttributeName();
            pointerIncrease();
            checkAttributeList();
        }
        else {
            checkAttributeName();
        }
    }

    public void checkAttributeName() {
        if (cmdArr[cmdArrPointer].length() == 0) {
            addErrorMessage("Attribute", "the");
        }
        else {
            checkName(cmdArr[cmdArrPointer]);
            compareAttributeName(cmdArr[cmdArrPointer]);
            if (cmdArr[0].equalsIgnoreCase("CREATE")) {
                if (cmdArr[cmdArrPointer].equalsIgnoreCase("ID")) {
                    addErrorMessage("Attribute's name", "the");
                }
                attributeList.add(cmdArr[cmdArrPointer]);
                if (attributeList.size() >= 2) {
                    for (int i = 0; i < attributeList.size(); i++) {
                        String strTemp = attributeList.get(i);
                        for (int j = 0; j < attributeList.size(); j++) {
                            if (i != j) {
                                if (strTemp.equalsIgnoreCase(attributeList.get(j))) {
                                    addErrorMessage("Attribute's name", "the");
                                }
                            }
                        }
                    }
                }
            }
            if (!checkPlainText()) {
                addErrorMessage("Attribute name", cmdArr[0]);
            }
        }
    }

    public void checkCondition1(String commandType) {
        pointerIncrease();
        if (cmdArr[cmdArrPointer].equalsIgnoreCase("WHERE")) {
            pointerIncrease();
            for (int i = cmdArrPointer; i < cmdArr.length; i++) {
                condition.add(cmdArr[i]);
            }
            checkCondition2();
        }
        else {
            addErrorMessage("'WHERE'", commandType);
        }
    }

    public void checkCondition2() {
        int leftBracketCnt = 0;
        int rightBracketCnt = 0;
        for (int i = cmdArrPointer; i < cmdArr.length; i++) {
            while (cmdArr[i].startsWith("(")) {
                cmdArr[i] = removeFirstChar(cmdArr[i]);
                leftBracketCnt++;
            }
            while (cmdArr[i].endsWith(")")) {
                cmdArr[i] = removeLastChar(cmdArr[i]);
                rightBracketCnt++;
            }
        }
        if (leftBracketCnt != rightBracketCnt) {
            addErrorMessage("The number of brackets", "the");
        }
        checkCondition3();
    }

    public void checkCondition3() {
        checkAttributeName();
        pointerIncrease();
        checkComparator();
        pointerIncrease();
        checkValue();
        if (cmdArrPointer < cmdArr.length-1) {
            checkBoolOperator();
            checkCondition3();
        }
    }

    public void checkBoolOperator() {
        pointerIncrease();
        if (cmdArr[cmdArrPointer].equalsIgnoreCase("AND")) {
            pointerIncrease();
        }
        else if (cmdArr[cmdArrPointer].equalsIgnoreCase("OR")) {
            pointerIncrease();
        }
        else {
            addErrorMessage("Bool operator (AND/OR)", cmdArr[0]);
        }
    }

    public void checkComparator() {
        String[] comparatorList = {"==", ">", "<", ">=", "<=", "!=", "LIKE", "like"};
        for (String s : comparatorList) {
            if (cmdArr[cmdArrPointer].equalsIgnoreCase(s)) {
                return;
            }
        }
        addErrorMessage("Comparator", cmdArr[0]);
    }


    public void checkLeftBracket() {
        if (cmdArr[cmdArrPointer].startsWith("(")) {
            cmdArr[cmdArrPointer] = removeFirstChar(cmdArr[cmdArrPointer]);
        }
        else {
            addErrorMessage("'('", cmdArr[0]);
        }
    }

    public void checkRightBracket() {
        if (cmdArr[cmdArr.length-1].endsWith(")")) {
            cmdArr[cmdArr.length-1] = removeLastChar(cmdArr[cmdArr.length-1]);
        }
        else {
            addErrorMessage("')'", cmdArr[0]);
        }
    }

    public void pointerIncrease() {
        if (cmdArrPointer < cmdArr.length-1) {
            cmdArrPointer++;
        }
        else {
            checkCmdResult = "[ERROR]:";
            checkCommandMessage.append("Command is incomplete, please check (or similar message !)\n");
        }
    }

    public boolean checkStringEquals(String checkString, String commandType) {
        pointerIncrease();
        if (cmdArr[cmdArrPointer].equalsIgnoreCase(checkString)) {
            pointerIncrease();
            return true;
        }
        else {
            addErrorMessage(checkString, commandType);
            return false;
        }
    }

    public void addErrorMessage(String error, String commandType) {
        checkCmdResult = "[ERROR]:";
        checkCommandMessage.append(error).append(" is missing/wrong/invalid in ").append(commandType)
                           .append(" command (or similar message !)\n");
        return;
    }

    public String removeFirstChar(String str) {
        str = str.substring(1);
        return str;
    }

    public String removeLastChar(String str) {
        str = str.substring(0, str.length()-1);
        return str;
    }

    public void checkName(String name) {
        for (String str:invalidNameList) {
            if (name.equalsIgnoreCase(str)) {
                addErrorMessage("DATABASE/TABLE/ATTRIBUTE's name", "the");
            }
        }
    }

    public void compareName(String type) {
        filePath = executor.getFilePath(type, cmdArr[cmdArrPointer]);
        File file = new File(filePath);
        if (cmdArr[0].equalsIgnoreCase("USE") ||
            cmdArr[0].equalsIgnoreCase("SELECT") ||
            cmdArr[0].equalsIgnoreCase("DROP") ||
            cmdArr[0].equalsIgnoreCase("ALTER") ||
            cmdArr[0].equalsIgnoreCase("INSERT") ||
            cmdArr[0].equalsIgnoreCase("UPDATE") ||
            cmdArr[0].equalsIgnoreCase("DELETE") ||
            cmdArr[0].equalsIgnoreCase("JOIN")) {
            if (!file.exists()) {
                addErrorMessage(type + "'s name", "the");
            }
        }
        else {
            if (file.exists()) {
                addErrorMessage(type + "'s name", "the");
            }
        }
    }

    public void compareAttributeName(String name) {
        boolean attributeExist = false;
        boolean needExist = true;
        if (cmdArr[0].equalsIgnoreCase("CREATE") ||
           (cmdArr[0].equalsIgnoreCase("ALTER") && cmdArr[3].equalsIgnoreCase("ADD"))) {
            needExist = false;
        }
        else if (cmdArr[0].equalsIgnoreCase("JOIN")) {
            boolean attributeExist1 = false;
            boolean attributeExist2 = false;
            String filePath2 = executor.getFilePath("TABLE", cmdArr[1]);
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath2))) {
                String firstLine = bufferedReader.readLine();
                String[] attributes = firstLine.split("\t");
                for (String str:attributes) {
                    if (cmdArr[5].equals(str)) {
                        attributeExist1 = true;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
                String firstLine = bufferedReader.readLine();
                String[] attributes = firstLine.split("\t");
                for (String str:attributes) {
                    if (cmdArr[7].equals(str)) {
                        attributeExist2 = true;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            attributeExist = attributeExist1 && attributeExist2;
        }
        else {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
                String firstLine = bufferedReader.readLine();
                String[] attributes = firstLine.split("\t");
                for (String str:attributes) {
                    if (name.equals(str)) {
                        attributeExist = true;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (needExist && !attributeExist) {
            addErrorMessage("Attribute's name", "the");
        }
    }
}
