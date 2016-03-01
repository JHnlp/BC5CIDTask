package gjh.bc5.dataset;

import java.util.List;

import gjh.bc5.utils.Sentence;

/**
 * Tokenizers take the text of a sentence and turn it into a Sentence object.
 * They do not fill in the Mentions in the sentence, and the Tokens which make
 * up the sentence have no fields besides the text field filled in.
 */
public interface Tokenizer {

	public void tokenize(Sentence sentence);

	public List<String> getTokens(String text);
}
