package gjh.bc5.features;

import java.util.Comparator;

import cc.mallet.classify.Classification;

public class MalletClassificationComparator
		implements Comparator<Classification> {

	public MalletClassificationComparator() {
	}

	public static void main(String[] args) {

	}

	public int compare(Classification o1, Classification o2) {
		String relaitonID1 = (String) o1.getInstance().getName();
		String relaitonID2 = (String) o2.getInstance().getName();

		return relaitonID1.compareTo(relaitonID2);
	}

}
