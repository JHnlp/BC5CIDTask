package gjh.bc5.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

public class TinyUtils {

	public static boolean isFileHasDuplicatedLines(File file, String encoding) {
		List<String> lines = null;
		try {
			lines = FileUtils.readLines(file, encoding);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Set<String> set_lines = new HashSet<String>(lines);
		if (lines.size() == set_lines.size())
			return false;

		return true;
	}

	// bubble sort, in descend sequence by the 2nd parameter.
	public static void bubbleSort(Object[] words, int[] frequencies) {
		for (int i = 0; i < words.length; i++) {
			for (int j = 0; j < words.length - i - 1; j++) {
				if (frequencies[j] < frequencies[j + 1]) {
					int temp = frequencies[j];
					Object tmp = words[j];
					frequencies[j] = frequencies[j + 1];
					words[j] = words[j + 1];
					frequencies[j + 1] = temp;
					words[j + 1] = tmp;
				}
			}
		}
	}

	public static void bubbleSort(Object[] words, Double[] frequencies) {
		for (int i = 0; i < words.length; i++) {
			for (int j = 0; j < words.length - i - 1; j++) {
				if (frequencies[j] < frequencies[j + 1]) {
					double temp = frequencies[j];
					Object tmp = words[j];
					frequencies[j] = frequencies[j + 1];
					words[j] = words[j + 1];
					frequencies[j + 1] = temp;
					words[j + 1] = tmp;
				}
			}
		}
	}

	// sort the map.entry in map in descend order according to the entry.value
	public static List<Map.Entry<String, Integer>> getDesendingOrderByMapValue_Integer(
			Map<String, Integer> map) {
		List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(
				map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1,
					Map.Entry<String, Integer> o2) {
				return (o2.getValue().compareTo(o1.getValue()));// 第二个减第一个，降序排列
			}
		});
		return list;
	}

	// sort the map.entry in map in descend order according to the entry.value
	public static List<Map.Entry<String, Double>> getDesendingOrderByMapValue_Double(
			Map<String, Double> map) {
		List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(
				map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1,
					Map.Entry<String, Double> o2) {
				return (o2.getValue().compareTo(o1.getValue()));// 第二个减第一个，降序排列
			}
		});
		return list;
	}

}
