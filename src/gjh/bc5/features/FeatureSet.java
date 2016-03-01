package gjh.bc5.features;

import java.util.ArrayList;

import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Alphabet;
import cc.mallet.types.LabelAlphabet;

/**
 * 
 * @author GJH
 * 
 */
public class FeatureSet {

	private SerialPipes pipe;

	public FeatureSet(Alphabet dataAlphabet, LabelAlphabet targetAlphabet,
			FeatrueConstructionMethod method) {
		switch (method) {
		case Entity_Level_Rough:
			pipe = createPipe_EntityLevel_Rough(dataAlphabet, targetAlphabet);
			return;
		case Entity_Level_Cooccur:
			pipe = createPipe_EntityLevel_Cooccur(dataAlphabet, targetAlphabet);
			return;
		case Entity_Level_Uncooccur:
			pipe = createPipe_EntityLevel_Uncooccur(dataAlphabet,
					targetAlphabet);
			return;
		case Mention_Level_Cooccur:
			pipe = createPipe_MentionLevel_Cooccur(dataAlphabet,
					targetAlphabet);
			return;
		case Mention_Level_Uncooccur:
			pipe = createPipe_MentionLevel_Uncooccur(dataAlphabet,
					targetAlphabet);
			return;
		default:
			throw new IllegalArgumentException("Unsupported format " + method);
		}
	}

	public enum FeatrueConstructionMethod {
		Entity_Level_Rough, Entity_Level_Cooccur, Entity_Level_Uncooccur, Mention_Level_Cooccur, Mention_Level_Uncooccur
	}

	private static SerialPipes createPipe_EntityLevel_Rough(
			Alphabet dataAlphabet, LabelAlphabet targetAlphabet) {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();
		// TODO Test feature variations
		// TODO Make configurable which features to use
		// TODO Try dropping redundant features

		pipes.add(new Relation2FeatureVector_EntityLevel_Rough(dataAlphabet,
				targetAlphabet));

		// pipes.add(new PrintInputAndTarget());

		return new SerialPipes(pipes);
	}

	private static SerialPipes createPipe_EntityLevel_Cooccur(
			Alphabet dataAlphabet, LabelAlphabet targetAlphabet) {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();

		pipes.add(new Relation2FeatureVector_EntityLevel_Cooccur(dataAlphabet,
				targetAlphabet));

		// pipes.add(new PrintInputAndTarget());

		return new SerialPipes(pipes);
	}

	private static SerialPipes createPipe_EntityLevel_Uncooccur(
			Alphabet dataAlphabet, LabelAlphabet targetAlphabet) {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();

		pipes.add(new Relation2FeatureVector_EntityLevel_Uncooccur(dataAlphabet,
				targetAlphabet));

		// pipes.add(new PrintInputAndTarget());

		return new SerialPipes(pipes);
	}

	private static SerialPipes createPipe_MentionLevel_Cooccur(
			Alphabet dataAlphabet, LabelAlphabet targetAlphabet) {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();

		pipes.add(new Relation2FeatureVector_MentionLevel_Cooccur(dataAlphabet,
				targetAlphabet));

		// pipes.add(new PrintInputAndTarget());

		return new SerialPipes(pipes);
	}

	private static SerialPipes createPipe_MentionLevel_Uncooccur(
			Alphabet dataAlphabet, LabelAlphabet targetAlphabet) {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();

		pipes.add(new Relation2FeatureVector_MentionLevel_Uncooccur(
				dataAlphabet, targetAlphabet));

		// pipes.add(new PrintInputAndTarget());

		return new SerialPipes(pipes);
	}

	public Pipe getPipe() {
		return pipe;
	}

}
