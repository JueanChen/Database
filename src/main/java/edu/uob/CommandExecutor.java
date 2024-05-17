package edu.uob;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class CommandExecutor {
    private String storageFolderPath;
    public String filePath;
    public String currentDatabase;
    public int conditionCnt;
    public int conditionFinalPointer;
    public List<String> conditionFinal = new ArrayList<>();
    public List<String> conditionFinal2 = new ArrayList<>();
    public List<List<Integer>> isCondition = new ArrayList<>();
    public List<Integer> isConditionFinal = new ArrayList<>();
    public List<Integer> notConditionFinal = new ArrayList<>();
    public List<List<String>> tableTemp = new ArrayList<>();
    public List<List<String>> tableTemp2 = new ArrayList<>();
    public List<List<String>> tablePrint = new ArrayList<>();
    public CommandExecutor() {
        currentDatabase = " ";
        reset();
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }
    }

    public void use(String[] cmdArr) {
        currentDatabase = cmdArr[1];
    }

    public void create(String[] cmdArr) {
        if (cmdArr[1].equalsIgnoreCase("DATABASE")) {
            filePath = getFilePath("DATABASE", cmdArr[2]);
            File database = new File(filePath);
            database.mkdir();
        }
        else {
            filePath = getFilePath("TABLE", cmdArr[2]);
            File table = new File(filePath);
            try {
                table.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (cmdArr.length > 3) {
                addAttribute(cmdArr, "id", 3);
            }
        }
    }

    public void addAttribute(String[] cmdArr, String firstStr, int startIndex) {
        FileWriter writer;
        StringBuffer str = new StringBuffer(firstStr);
        for (int i = startIndex; i < cmdArr.length; i++) {
            str.append("\t");
            str.append(cmdArr[i]);
        }
        str.append("\n");
        try {
            writer = new FileWriter(filePath, true);
            writer.write(str.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void insert(String[] cmdArr) {
        String lastLine = "";
        String id = "id";
        StringBuffer oldId = new StringBuffer();
        int newId;
        filePath = getFilePath("TABLE", cmdArr[2]);
        File table = new File(filePath);
        if (table.length() != 0) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
                String currentLine = "";
                while ((currentLine = bufferedReader.readLine()) != null) {
                    lastLine = currentLine;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            for (int i = 0; i < lastLine.length(); i++) {  // Get the last id
                if (lastLine.charAt(i) == '\t') {
                    break;
                }
                oldId.append(lastLine.charAt(i));
            }
            if (oldId.toString().equals("id")) {
                newId = 1;
            } else {
                newId = Integer.parseInt(oldId.toString()) + 1;
            }
            id = String.valueOf(newId);
        }
        addAttribute(cmdArr, id, 4);
    }

    public void select(String[] cmdArr, int tableIndex, List<String> condition) {
        reset();
        readTable(cmdArr[tableIndex]);
        if (!cmdArr[1].equals("*")) {
            boolean attributeSelect = false;
            List<String> selctAttributeList = new ArrayList<>();
            for (int i = 1; i < cmdArr.length; i++) {
                if (cmdArr[i].equalsIgnoreCase("FROM")) {
                    break;
                }
                selctAttributeList.add(cmdArr[i]);
            }
            List<String> attributeList = tableTemp.get(0);
            int removeCnt = 0;
            for (int i = 0; i < attributeList.size(); i++) {
                for (int j = 0; j < selctAttributeList.size(); j++) {
                    if (attributeList.get(i).equals(selctAttributeList.get(j))) {
                        attributeSelect = true;
                    }
                }
                if (!attributeSelect) {
                    for (List<String> row : tablePrint) {
                        row.remove(i-removeCnt);
                    }
                    removeCnt++;
                }
                attributeSelect = false;
            }
        }
        if (tableIndex < cmdArr.length-1) {
            checkCondition(condition);
            checkCondition3();
            for (int i = 0; i < notConditionFinal.size(); i++) {
                tablePrint.remove(notConditionFinal.get(i)-i);
            }
        }
    }

    public void update(String[] cmdArr, List<String> condition) {
        reset();
        readTable(cmdArr[1]);
        List<String> updateAttribute = new ArrayList<>();
        List<String> updateValue = new ArrayList<>();
        List<Integer> updateAttributeIndex = new ArrayList<>();
        for (int i = 3; !cmdArr[i].equalsIgnoreCase("WHERE"); i+=3) {
            updateAttribute.add(cmdArr[i]);
            updateValue.add(cmdArr[i+2]);
        }
        for (int i = 0; i < tableTemp.get(0).size(); i++) {
            for (int j = 0; j < updateAttribute.size(); j++) {
                if (tableTemp.get(0).get(i).equals(updateAttribute.get(j))) {
                    updateAttributeIndex.add(i);
                }
            }
        }
        checkCondition(condition);
        checkCondition3();
        for (int i = 0; i < isConditionFinal.size(); i++) {
            for (int j = 0; j < updateAttributeIndex.size(); j++) {
                tableTemp.get(isConditionFinal.get(i))
                         .set(updateAttributeIndex.get(j), updateValue.get(j));
            }
        }
        writeTable(cmdArr[1]);
    }

    public void alter(String[] cmdArr) {
        reset();
        readTable(cmdArr[2]);
        if (cmdArr[3].equalsIgnoreCase("ADD")) {
            tableTemp.get(0).add(cmdArr[4]);
            for (int i = 1; i < tableTemp.size(); i++) {
                tableTemp.get(i).add("");
            }
        }
        else {
            int dropIndex = 1;
            for (int i = 0; i < tableTemp.get(0).size(); i++) {
                if (tableTemp.get(0).get(i).equals(cmdArr[4])) {
                    dropIndex = i;
                }
            }
            for (List<String> row : tableTemp) {
                row.remove(dropIndex);
            }
        }
        writeTable(cmdArr[2]);
    }

    public void delete(String[] cmdArr, List<String> condition) {
        reset();
        readTable(cmdArr[2]);
        checkCondition(condition);
        checkCondition3();
        for (int i = 0; i < isConditionFinal.size(); i++) {
            tableTemp.remove(isConditionFinal.get(i)-i);
        }
        writeTable(cmdArr[2]);
    }

    public void drop(String[] cmdArr) {
        if (cmdArr[1].equalsIgnoreCase("DATABASE")) {
            filePath = getFilePath("DATABASE", cmdArr[2]);
            File database = new File(filePath);
            File[] list = database.listFiles();
            for (File f:list) {
                f.delete();
            }
            database.delete();
        }
        else {
            filePath = getFilePath("TABLE", cmdArr[2]);
            File table = new File(filePath);
            table.delete();
        }
    }

    public void join(String[] cmdArr) {
        reset();
        readTwoTables(cmdArr[1], cmdArr[3]);
        List<String> reference = new ArrayList<>();
        for (List<String> row : tableTemp) {
            row.remove(0);
        }
        for (List<String> row : tableTemp2) {
            row.remove(0);
        }
        for (int i = 0; i < tableTemp.get(0).size(); i++) {
            if (tableTemp.get(0).get(i).equals(cmdArr[5])) {
                for (List<String> row : tableTemp) {
                    reference.add(row.remove(i));
                }
            }
        }
        reference.remove(0);
        for (int i = 0; i < tableTemp2.get(0).size(); i++) {
            if (tableTemp2.get(0).get(i).equals(cmdArr[7])) {
                for (List<String> row : tableTemp2) {
                    row.remove(i);
                }
            }
        }
        for (int i = 0; i < tableTemp.get(0).size(); i++) {
            String temp = tableTemp.get(0).get(i);
            tableTemp.get(0).set(i, cmdArr[1] + "." + temp);
        }
        for (int i = 0; i < tableTemp2.get(0).size(); i++) {
            String temp = tableTemp2.get(0).get(i);
            tableTemp2.get(0).set(i, cmdArr[3] + "." + temp);
        }
        tablePrint.add(new ArrayList<>());
        tablePrint.get(0).add("id");
        for (int i = 0; i < tableTemp.get(0).size(); i++) {
            tablePrint.get(0).add(tableTemp.get(0).get(i));
        }
        for (int i = 0; i < tableTemp2.get(0).size(); i++) {
            tablePrint.get(0).add(tableTemp2.get(0).get(i));
        }
        for (int i = 1; i < Math.min(tableTemp.size(), tableTemp2.size()); i++) {
            tablePrint.add(new ArrayList<>());
            tablePrint.get(i).add(Integer.toString(i));
            for (int j = 0; j < tableTemp.get(i).size(); j++) {
                tablePrint.get(i).add(tableTemp.get(i).get(j));
            }
            for (int j = 0; j < tableTemp2.get(i).size(); j++) {
                tablePrint.get(i).add(tableTemp2.get(Integer.parseInt(reference.get(i-1))).get(j));
            }
        }
    }

    public String getFilePath(String type, String fileName) {
        if (type.equals("DATABASE")) {
            return storageFolderPath + File.separator + fileName;
        }
        else {
            if (currentDatabase.equals(" ")) {
                return storageFolderPath + File.separator + fileName + ".tab";
            } else {
                return storageFolderPath + File.separator +
                        currentDatabase + File.separator + fileName + ".tab";
            }
        }
    }

    public void readTable(String tableName) {
        int lineCnt = 0;
        String filePath = getFilePath("TABLE", tableName);
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            String currentLine = "";
            while ((currentLine = bufferedReader.readLine()) != null) {
                String[] parts = currentLine.split("\t", -1);
                tableTemp.add(new ArrayList<>());
                tablePrint.add(new ArrayList<>());
                for(String str:parts) {
                    if (str.endsWith("\n")) {
                        str = str.substring(0, str.length()-1);
                    }
                    tableTemp.get(lineCnt).add(str);
                    tablePrint.get(lineCnt).add(str);
                }
                lineCnt++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void readTwoTables(String tableName1, String tableName2) {
        int lineCnt = 0;
        String filePath1 = getFilePath("TABLE", tableName1);
        String filePath2 = getFilePath("TABLE", tableName2);
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath1))) {
            String currentLine = "";
            while ((currentLine = bufferedReader.readLine()) != null) {
                String[] parts = currentLine.split("\t");
                tableTemp.add(new ArrayList<>());
                for(String str:parts) {
                    if (str.endsWith("\n")) {
                        str = str.substring(0, str.length()-1);
                    }
                    tableTemp.get(lineCnt).add(str);
                }
                lineCnt++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        lineCnt = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath2))) {
            String currentLine = "";
            while ((currentLine = bufferedReader.readLine()) != null) {
                String[] parts = currentLine.split("\t");
                tableTemp2.add(new ArrayList<>());
                for(String str:parts) {
                    if (str.endsWith("\n")) {
                        str = str.substring(0, str.length()-1);
                    }
                    tableTemp2.get(lineCnt).add(str);
                }
                lineCnt++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void writeTable(String tableName) {
        String filePath = getFilePath("TABLE", tableName);
        try {
            FileWriter writeTable = new FileWriter(filePath, false);
            for(List<String> row : tableTemp) {
                for(String element : row) {
                    writeTable.write(element + "\t");
                }
                writeTable.write("\n");
            }
            writeTable.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void checkCondition(List<String> condition) {
        for (int i = 0; i < condition.size(); i++) {
            if (condition.get(i).startsWith("(")) {
                while (condition.get(i).startsWith("(")) {
                    conditionFinal.add("(");
                    condition.set(i, condition.get(i).substring(1));
                }
                conditionFinal.add(condition.get(i));
            }
            else if (condition.get(i).startsWith("'")) {
                condition.set(i, condition.get(i).substring(1));
                condition.set(i, condition.get(i).substring(0, condition.get(i).length()-1));
                conditionFinal.add(condition.get(i));
            }
            else if (condition.get(i).endsWith(")")) {
                int cnt = 0;
                while (condition.get(i).endsWith(")")) {
                    cnt++;
                    condition.set(i, condition.get(i).substring(0, condition.get(i).length()-1));
                }
                conditionFinal.add(condition.get(i));
                for (int j = 0; j < cnt; j++) {
                    conditionFinal.add(")");
                }
            }
            else {
                conditionFinal.add(condition.get(i));
            }
        }
        checkCondition1();
    }

    public void checkCondition1() {
        if (conditionFinalPointer == conditionFinal.size()) {
            return;
        }
        if (conditionFinal.get(conditionFinalPointer).equals("(")) {
            conditionFinalPointer++;
            conditionFinal2.add("(");
        }
        else if (conditionFinal.get(conditionFinalPointer).equalsIgnoreCase("AND")) {
            conditionFinalPointer++;
            conditionFinal2.add("AND");
        }
        else if (conditionFinal.get(conditionFinalPointer).equalsIgnoreCase("OR")) {
            conditionFinalPointer++;
            conditionFinal2.add("OR");
        }
        else if (conditionFinal.get(conditionFinalPointer).equals(")")) {
            conditionFinalPointer++;
            conditionFinal2.add(")");
        }
        else {
            conditionFinal2.add(Integer.toString(conditionCnt));
            checkCondition2(conditionFinal.get(conditionFinalPointer),
                            conditionFinal.get(conditionFinalPointer+1),
                            conditionFinal.get(conditionFinalPointer+2));
            conditionFinalPointer += 3;
        }
        checkCondition1();
    }

    public void checkCondition2(String attributeName, String comparator, String value) {
        int attributeIndex = 0;
        isCondition.add(new ArrayList<>());
        conditionCnt++;
        for (int i = 0; i < tableTemp.get(0).size(); i++) {
            if (tableTemp.get(0).get(i).equals(attributeName)) {
                attributeIndex = i;
            }
        }
        for (int i = 1; i < tableTemp.size(); i++) {
            String temp = tableTemp.get(i).get(attributeIndex);
            switch (comparator) {
                case "==" -> {
                    if (temp.equals(value)) {
                        isCondition.get(conditionCnt-1).add(i);
                    }
                }
                case "!=" -> {
                    if (!temp.equals(value)) {
                        isCondition.get(conditionCnt-1).add(i);
                    }
                }
                case "LIKE", "like" -> {
                    if (temp.contains(value)) {
                        isCondition.get(conditionCnt-1).add(i);
                    }
                }
                case ">" -> {
                    if (Double.parseDouble(temp) > Double.parseDouble(value)) {
                        isCondition.get(conditionCnt-1).add(i);
                    }
                }
                case "<" -> {
                    if (Double.parseDouble(temp) < Double.parseDouble(value)) {
                        isCondition.get(conditionCnt-1).add(i);
                    }
                }
                case ">=" -> {
                    if (Double.parseDouble(temp) >= Double.parseDouble(value)) {
                        isCondition.get(conditionCnt-1).add(i);
                    }
                }
                case "<=" -> {
                    if (Double.parseDouble(temp) <= Double.parseDouble(value)) {
                        isCondition.get(conditionCnt-1).add(i);
                    }
                }
                default -> {
                }
            }
        }
    }

    public void checkCondition3() {
        for (int i = 1; i < tableTemp.size(); i++) {
            if (conditionPass(i)) {
                isConditionFinal.add(i);
            }
            else {
                notConditionFinal.add(i);
            }
        }
    }

    public boolean conditionPass(int i) {
        List<Boolean> ifConditionPass = new ArrayList<>();
        Stack<Boolean> numStack = new Stack<>();
        Stack<String> operStack = new Stack<>();
        boolean flag;
        for (int j = 0; j < conditionCnt; j++) {
            flag = false;
            for (int k = 0; k < isCondition.get(j).size(); k++) {
                if (i == isCondition.get(j).get(k)) {
                    flag = true;
                }
            }
            ifConditionPass.add(flag);
        }
        for (int j = 0; j < conditionFinal2.size(); j++) {
            String temp = conditionFinal2.get(j);
            if (temp.equals("(")) {
                operStack.push(temp);
            }
            else if (temp.equals(")")) {
                while (!operStack.peek().equals("(")) {
                    String operator = operStack.pop();
                    boolean operand2 = numStack.pop();
                    boolean operand1 = numStack.pop();
                    boolean result = performOperation(operand1, operand2, operator);
                    numStack.push(result);
                }
                operStack.pop();
            }
            else if (temp.equalsIgnoreCase("AND") || temp.equalsIgnoreCase("OR")) {
                while (!operStack.isEmpty() && hasHigherPriority(temp, operStack.peek())) {
                    String operator = operStack.pop();
                    boolean operand2 = numStack.pop();
                    boolean operand1 = numStack.pop();
                    boolean result = performOperation(operand1, operand2, operator);
                    numStack.push(result);
                }
                operStack.push(temp);
            }
            else {
                int cnt = Integer.parseInt(temp);
                numStack.push(ifConditionPass.get(cnt));
            }
        }
        while (!operStack.isEmpty()) {
            String operator = operStack.pop();
            boolean operand2 = numStack.pop();
            boolean operand1 = numStack.pop();
            boolean result = performOperation(operand1, operand2, operator);
            numStack.push(result);
        }
        return numStack.pop();
    }

    private boolean performOperation(boolean operand1, boolean operand2, String operator) {
        if (operator.equalsIgnoreCase("AND")) {
            return operand1 && operand2;
        }
        else {
            return operand1 || operand2;
        }
    }

    private boolean hasHigherPriority(String op1, String op2) {
        return (op1.equalsIgnoreCase("AND") && op2.equalsIgnoreCase("OR"));
    }

    public String printResult() {
        StringBuffer result = new StringBuffer("[OK]\n");
        for (List<String> row:tablePrint) {
            for (String element:row) {
                result.append(element + '\t');
            }
            result.append('\n');
        }
        return result.toString();
    }

    public void reset() {
        conditionCnt = 0;
        conditionFinalPointer = 0;
        conditionFinal = new ArrayList<>();
        conditionFinal2 = new ArrayList<>();
        isCondition = new ArrayList<>();
        isConditionFinal = new ArrayList<>();
        notConditionFinal = new ArrayList<>();
        tableTemp = new ArrayList<>();
        tableTemp2 = new ArrayList<>();
        tablePrint = new ArrayList<>();
    }
}
