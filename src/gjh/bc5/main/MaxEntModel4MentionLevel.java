package gjh.bc5.main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import cc.mallet.classify.Classifier;
import cc.mallet.types.Alphabet;
import cc.mallet.types.LabelAlphabet;

public class MaxEntModel4MentionLevel implements Cloneable, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 872019636141618160L;
	private Alphabet dataAlphabet_Cooccur;
	private LabelAlphabet targetAlphabet_Cooccur;
	private Classifier maxentclassifier_Cooccur;

	private Alphabet dataAlphabet_Uncooccur;
	private LabelAlphabet targetAlphabet_Uncooccur;
	private Classifier maxentclassifier_Uncooccur;

	public MaxEntModel4MentionLevel(Alphabet dataAlphabet_Cooccur,
			LabelAlphabet targetAlphabet_Cooccur,
			Classifier maxentclassifier_Cooccur,
			Alphabet dataAlphabet_Uncooccur,
			LabelAlphabet targetAlphabet_Uncooccur,
			Classifier maxentclassifier_Uncooccur) {
		super();

		if (dataAlphabet_Cooccur == null)
			throw new IllegalArgumentException(
					"The data alphabet of cooccurrence should not be null!");
		this.dataAlphabet_Cooccur = dataAlphabet_Cooccur;

		if (targetAlphabet_Cooccur == null)
			throw new IllegalArgumentException(
					"The label alphabet of cooccurrence should not be null!");
		this.targetAlphabet_Cooccur = targetAlphabet_Cooccur;

		if (maxentclassifier_Cooccur == null)
			throw new IllegalArgumentException(
					"The classifier of cooccurrence should not be null!");
		this.maxentclassifier_Cooccur = maxentclassifier_Cooccur;

		if (dataAlphabet_Uncooccur == null)
			throw new IllegalArgumentException(
					"The data alphabet of uncooccurrence should not be null!");
		this.dataAlphabet_Uncooccur = dataAlphabet_Uncooccur;

		if (targetAlphabet_Uncooccur == null)
			throw new IllegalArgumentException(
					"The label alphabet of uncooccurrence should not be null!");
		this.targetAlphabet_Uncooccur = targetAlphabet_Uncooccur;

		if (maxentclassifier_Uncooccur == null)
			throw new IllegalArgumentException(
					"The classifier of uncooccurrence should not be null!");
		this.maxentclassifier_Uncooccur = maxentclassifier_Uncooccur;
	}

	public MaxEntModel4MentionLevel(File cooccur_data_dict_file,
			File cooccur_label_dict_file, File cooccur_classifier_file,
			File uncooccur_data_dict_file, File uncooccur_label_dict_file,
			File uncooccur_classifier_file) {

		// cooccur model
		dataAlphabet_Cooccur = loadDataDictionary(cooccur_data_dict_file);

		targetAlphabet_Cooccur = loadLabelDictionary(cooccur_label_dict_file);

		maxentclassifier_Cooccur = loadClassifier(cooccur_classifier_file);

		// uncooccur model
		dataAlphabet_Uncooccur = loadDataDictionary(uncooccur_data_dict_file);

		targetAlphabet_Uncooccur = loadLabelDictionary(
				uncooccur_label_dict_file);

		maxentclassifier_Uncooccur = loadClassifier(uncooccur_classifier_file);
	}

	public MaxEntModel4MentionLevel(File loading_model_folder) {
		loadModel(loading_model_folder);
	}

	@Override
	protected Object clone() {
		try {
			// save the object to a byte array
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bout);
			out.writeObject(this);
			out.close();

			// read a clone of the object from the byte array
			ByteArrayInputStream bin = new ByteArrayInputStream(
					bout.toByteArray());
			ObjectInputStream in = new ObjectInputStream(bin);
			Object ret = in.readObject();
			in.close();

			return ret;
		} catch (Exception e) {
			return null;
		}
	}

	public Alphabet getDataAlphabet_Cooccur() {
		return dataAlphabet_Cooccur;
	}

	public LabelAlphabet getTargetAlphabet_Cooccur() {
		return targetAlphabet_Cooccur;
	}

	public Classifier getMaxentclassifier_Cooccur() {
		return maxentclassifier_Cooccur;
	}

	public Alphabet getDataAlphabet_Uncooccur() {
		return dataAlphabet_Uncooccur;
	}

	public LabelAlphabet getTargetAlphabet_Uncooccur() {
		return targetAlphabet_Uncooccur;
	}

	public Classifier getMaxentclassifier_Uncooccur() {
		return maxentclassifier_Uncooccur;
	}

	public void saveAlphabetDictionary(Alphabet dict, File save_file) {
		PrintWriter outAlphabet = null;

		try {
			try {
				outAlphabet = new PrintWriter(save_file, "utf-8");
				dict.dump(outAlphabet);

				outAlphabet.flush();
			} finally {
				outAlphabet.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public Alphabet loadDataDictionary(File dict_file) {
		if (!dict_file.exists() || !dict_file.isFile())
			throw new IllegalArgumentException(
					"The data dictionary file does not exist!");

		Alphabet dict = new Alphabet();
		Scanner in = null;
		try {
			try {
				in = new Scanner(dict_file, "utf-8");
				String line = "";
				while (in.hasNextLine()) {
					line = in.nextLine();
					String tmp[] = line.split(" => ");
					String entry = tmp[1];
					dict.lookupIndex(entry);
				}
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return dict;
	}

	public LabelAlphabet loadLabelDictionary(File dict_file) {
		if (!dict_file.exists() || !dict_file.isFile())
			throw new IllegalArgumentException(
					"The label dictionary file does not exist!");

		LabelAlphabet dict = new LabelAlphabet();
		Scanner in = null;
		try {
			try {
				in = new Scanner(dict_file, "utf-8");
				String line = "";
				while (in.hasNextLine()) {
					line = in.nextLine();
					String tmp[] = line.split(" => ");
					String entry = tmp[1];
					dict.lookupIndex(entry);
				}
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return dict;
	}

	// write a trained classifier to disk
	public void saveClassifier(Classifier classifier, File save_file) {
		ObjectOutputStream oos = null;
		try {
			try {
				oos = new ObjectOutputStream(new FileOutputStream(save_file));
				oos.writeObject(classifier);
				oos.flush();
			} finally {
				oos.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// restore a saved classifier
	public Classifier loadClassifier(File loading_file) {
		if (!loading_file.exists() || !loading_file.isFile())
			throw new IllegalArgumentException(
					"The classifier file does not exist!");

		// Here we load a serialized classifier from a file.
		Classifier classifier = null;
		ObjectInputStream ois = null;
		try {
			try {
				ois = new ObjectInputStream(new FileInputStream(loading_file));
				classifier = (Classifier) ois.readObject();
			} finally {
				ois.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return classifier;
	}

	public void loadModel(File loading_folder) {
		if (!loading_folder.exists() || !loading_folder.isDirectory())
			throw new IllegalArgumentException(
					"The classification model directory does not exist!");

		File cooccur_data_dict_file = new File(loading_folder.getPath() + "/"
				+ "MaxEnt_DataAlphabet_Cooccur.dict");
		if (!cooccur_data_dict_file.exists()
				|| !cooccur_data_dict_file.isFile())
			throw new IllegalArgumentException(
					"The cooccur data alphabet file does not exist!");

		File cooccur_label_dict_file = new File(loading_folder.getPath() + "/"
				+ "MaxEnt_LabelAlphabet_Cooccur.dict");
		if (!cooccur_label_dict_file.exists()
				|| !cooccur_label_dict_file.isFile())
			throw new IllegalArgumentException(
					"The cooccur label alphabet file does not exist!");

		File cooccur_classifier_file = new File(
				loading_folder.getPath() + "/" + "MaxEnt_Cooccur.model");
		if (!cooccur_classifier_file.exists()
				|| !cooccur_classifier_file.isFile())
			throw new IllegalArgumentException(
					"The cooccur classifier file does not exist!");

		File uncooccur_data_dict_file = new File(loading_folder.getPath() + "/"
				+ "MaxEnt_DataAlphabet_Uncooccur.dict");
		if (!uncooccur_data_dict_file.exists()
				|| !uncooccur_data_dict_file.isFile())
			throw new IllegalArgumentException(
					"The uncooccur data alphabet file does not exist!");

		File uncooccur_label_dict_file = new File(loading_folder.getPath() + "/"
				+ "MaxEnt_LabelAlphabet_Uncooccur.dict");
		if (!uncooccur_label_dict_file.exists()
				|| !uncooccur_label_dict_file.isFile())
			throw new IllegalArgumentException(
					"The uncooccur label alphabet file does not exist!");

		File uncooccur_classifier_file = new File(
				loading_folder.getPath() + "/" + "MaxEnt_Uncooccur.model");
		if (!uncooccur_classifier_file.exists()
				|| !uncooccur_classifier_file.isFile())
			throw new IllegalArgumentException(
					"The uncooccur classifier file does not exist!");

		// cooccur model
		dataAlphabet_Cooccur = loadDataDictionary(cooccur_data_dict_file);

		targetAlphabet_Cooccur = loadLabelDictionary(cooccur_label_dict_file);

		maxentclassifier_Cooccur = loadClassifier(cooccur_classifier_file);

		// uncooccur model
		dataAlphabet_Uncooccur = loadDataDictionary(uncooccur_data_dict_file);

		targetAlphabet_Uncooccur = loadLabelDictionary(
				uncooccur_label_dict_file);

		maxentclassifier_Uncooccur = loadClassifier(uncooccur_classifier_file);
	}

	public void saveModel(File output_model_folder) {
		if (!output_model_folder.exists() || !output_model_folder.isDirectory())
			output_model_folder.mkdirs();

		File cooccur_data_dict_file = new File(output_model_folder.getPath()
				+ "/" + "MaxEnt_DataAlphabet_Cooccur.dict");
		saveAlphabetDictionary(dataAlphabet_Cooccur, cooccur_data_dict_file);

		File cooccur_label_dict_file = new File(output_model_folder.getPath()
				+ "/" + "MaxEnt_LabelAlphabet_Cooccur.dict");
		saveAlphabetDictionary(targetAlphabet_Cooccur, cooccur_label_dict_file);

		File cooccur_classifier_file = new File(
				output_model_folder.getPath() + "/" + "MaxEnt_Cooccur.model");
		saveClassifier(maxentclassifier_Cooccur, cooccur_classifier_file);

		File uncooccur_data_dict_file = new File(output_model_folder.getPath()
				+ "/" + "MaxEnt_DataAlphabet_Uncooccur.dict");
		saveAlphabetDictionary(dataAlphabet_Uncooccur,
				uncooccur_data_dict_file);

		File uncooccur_label_dict_file = new File(output_model_folder.getPath()
				+ "/" + "MaxEnt_LabelAlphabet_Uncooccur.dict");
		saveAlphabetDictionary(targetAlphabet_Uncooccur,
				uncooccur_label_dict_file);

		File uncooccur_classifier_file = new File(
				output_model_folder.getPath() + "/" + "MaxEnt_Uncooccur.model");
		saveClassifier(maxentclassifier_Uncooccur, uncooccur_classifier_file);
	}

	public static void main(String[] args) {
		MaxEntModel4MentionLevel model = new MaxEntModel4MentionLevel(new File(
				"DifferentMethods/SplitProcessing_MentionLevel/process3"));

		model.saveModel(
				new File("DifferentMethods/SplitProcessing_MentionLevel/7777"));

		System.out.println();
	}

}
