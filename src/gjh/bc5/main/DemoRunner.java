package gjh.bc5.main;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/**
 * 
 * @author GJH
 * 
 */
public class DemoRunner {

	public static void demo1() throws IOException {
		System.out.println(
				"This demo is trained by the training set and tested on the development set using gold entity annotaion!");
		System.out.println();

		FileUtils.cleanDirectory(new File("demo/examples/result"));

		BC5Runner.predictingOnDemos(
				new File("trained_models/model_trained_by_training_set"),
				new File("demo/examples/dataset"),
				"CDR_DevelopmentSet.PubTator.txt",
				new File("demo/examples/result"));
		BC5Runner.postProcessing("demo/examples/result/CID_result.txt",
				"demo/examples/result/CID_result_after_post_processing.txt",
				BC5Runner.getMeshThesaurus());

		BC5Runner.bc5CIDEvaluation("relation", "CID",
				new File("Corpus/CDR_DevelopmentSet.PubTator.txt"), new File(
						"demo/examples/result/CID_result_after_post_processing.txt"));
	}

	public static void demo2() throws IOException {
		System.out.println(
				"This demo is trained by the training+development set and tested on the testing set using gold entity annotaion!");
		System.out.println();

		FileUtils.cleanDirectory(new File("demo/examples/result"));

		BC5Runner.predictingOnDemos(
				new File(
						"trained_models/model_trained_by_training_and_development_set"),
				new File("demo/examples/dataset"), "CDR_TestSet.PubTator.txt",
				new File("demo/examples/result"));
		BC5Runner.postProcessing("demo/examples/result/CID_result.txt",
				"demo/examples/result/CID_result_after_post_processing.txt",
				BC5Runner.getMeshThesaurus());

		BC5Runner.bc5CIDEvaluation("relation", "CID",
				new File("Corpus/CDR_TestSet.PubTator.txt"), new File(
						"demo/examples/result/CID_result_after_post_processing.txt"));
	}

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("*** Usage information for demo ***");
			System.out.println(
					"-D1: trained by the trainging set and tested on the development set");
			System.out.println(
					"-D2: trained by the trainging+development set and tested on the testing set");
		} else if (args[0].equals("-D1")) {
			demo1();
		} else if (args[0].equals("-D2")) {
			demo2();
		} else {
			System.out.println(
					"Demo input format: gjh.bc5.main.DemoRunner -D1 or -D2\n");
		}

		System.out.println("\n**** End ****\n");
	}
}