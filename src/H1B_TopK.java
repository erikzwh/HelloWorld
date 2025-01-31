import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class H1B_TopK {

	public static void main(String[] args) {
		String inputFilePath = args[0];
		String outputOccupPath = args[1];
		String outputStatePath = args[2];
		H1B_TopK selector = new H1B_TopK();
		selector.selectTopK(inputFilePath, outputStatePath, outputOccupPath, 10);
	}
	
	private boolean selectTopK(String inputFilePath, String outputStatePath, String outputOccupPath, int numK) {
		
		//Create two maps to store (state, count) and (job, count) pairs
		Map<String, Integer> stateMap = new HashMap<>();
		Map<String, Integer> jobMap = new HashMap<>();
		
		//Read raw data, count totalNumber of certified cases and save h1b statistics in the maps 
		int certifiedNum = createMapsFromRawData(inputFilePath, stateMap, jobMap);
		if (certifiedNum == 0) return false;
		
		//Get lists stored the sorted top K keys in the hash maps
		List<String> topKstateKeyList = TopKSelector.getTopKKeysFromMap(stateMap,numK);
		List<String> topKjobKeyList = TopKSelector.getTopKKeysFromMap(jobMap,numK);
		
		//Compute the percentage of top K keys in the whole certified cases
		List<String> topKstatePercent = computePercent(topKstateKeyList, stateMap, certifiedNum);
		List<String> topKjobPercent = computePercent(topKjobKeyList, jobMap, certifiedNum);
		
		//Save the information of top K entries as local text files
		saveAsText(topKjobKeyList, topKjobPercent, jobMap, outputOccupPath, "job");
		saveAsText(topKstateKeyList, topKstatePercent, stateMap, outputStatePath,"state");
		
		return true;
		
	}
	
	private int createMapsFromRawData(String inputFilePath, Map<String, Integer> stateMap, Map<String, Integer> jobMap) {
		
		int certifiedNum = 0;
		
		//read contents from input.csv
		try (BufferedReader bfreader = new BufferedReader(new FileReader(inputFilePath))) {
			
			//read the header
			String header = bfreader.readLine();
			
			//get target field indexes from header line
			//targetCols = [index of status, index of work state, index of occupation name]
			int[] targetCols = getTargetColIndices(header, new String[]{".*STATUS.*", ".*SOC_NAME.*", ".*WORK.*STATE.*"});
			
			//read and process data case by case
			String line = "";
			while((line = bfreader.readLine()) != null) {
				
				//prepocessEntry() removes the semicolons in quotes and all quotations.
				String[] fields = preprocessEntry(line).split(";");
				
				//If the case is not certified, ignore it.
				if (!fields[targetCols[0]].toUpperCase().trim().equals("CERTIFIED")){
					continue;
				}

				//extract work state and occupation name
				String job = fields[targetCols[1]];
				String state = fields[targetCols[2]];
				
				//update the number of certified cases and frequencies of states and jobs in the maps
				certifiedNum++;
				stateMap.put(state, stateMap.getOrDefault(state, 0) + 1);
				jobMap.put(job,jobMap.getOrDefault(job, 0) + 1);
			}
			
			bfreader.close();
			
			//print the map sizes for testing
			System.out.print("Total number of different certified states is " + stateMap.size() + "; of jobs is: " + jobMap.size() + "\n");
			System.out.print("Total number of certified cases is " + certifiedNum);
			
			return certifiedNum;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return certifiedNum;
	}
	
	// search the indexes of the target fields based on keywords
	private int[] getTargetColIndices(String header, String[] keyPatterns) {
		
		if (header == null) {
			System.out.println("Empty header!");
			return null;
		}
		
		int[] targetColumns = new int[keyPatterns.length];
		
		for (int i = 0; i < targetColumns.length; i++) {
			targetColumns[i] = -1;
		}
		
		String[] atrributes = header.toUpperCase().split(";");
		
		for (int i = 0; i < atrributes.length; i++) {
			for(int j = 0; j < keyPatterns.length; j++) {
				if (atrributes[i].matches(keyPatterns[j]) && targetColumns[j] == -1) {
					targetColumns[j] = i;
				}
			}
		}
		
		return targetColumns;
	}
	
	private String preprocessEntry(String line) {
		
		char[] charArr = line.toUpperCase().toCharArray();
		int quoteNum = 0;
		
		//replace semicolons(;) in quotes with space
		for (int i = 0; i < charArr.length; i++) {
			if (charArr[i] == ';' && quoteNum % 2 == 1) {
				charArr[i] = ' ';
			}
			if (charArr[i] == '\"') {
				quoteNum++;
			}
		}
		
		//remove all quotations in the string
		String line1 = new String(charArr);
		String resLine = line1.replaceAll("\"", "");
		return resLine;
	}
	
	private List<String> computePercent(List<String> keyList, Map<String, Integer> map, int totalNum){
		
		List<String> result = new ArrayList<>();
		
		if (totalNum == 0) {
			return result;
		}
		for (String key : keyList) {
			Double percent = (double)map.get(key)/totalNum * 100;
			//rounded off to 1 decimal place.
			result.add(String.format("%.1f", percent));
		}
		return result;
	}
	
	private boolean saveAsText(List<String> topKKeyList, List<String> percentList, Map<String, Integer> map, String filePath, String sortField) {
		try {
			
			//delete old text file first.
			File f1 = new File(filePath);
			if (f1.isFile()) {
				f1.delete();
			}
			
			//write header
			BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
			if (sortField.contains("job")) {
				writer.append("TOP_OCCUPATIONS;NUMBER_CERTIFIED_APPLICATIONS;PERCENTAGE");
			} else if (sortField.contains("state")) {
				writer.append("TOP_STATES;NUMBER_CERTIFIED_APPLICATIONS;PERCENTAGE");
			}
			
			//write top k entries
			for (int i = 0; i < topKKeyList.size(); i++) {
				writer.newLine();
				writer.append(topKKeyList.get(i) + ";" + map.get(topKKeyList.get(i)) + ";" + percentList.get(i) + "%");
			}
			
			writer.close();
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
