package gjh.bc5.dataset;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.configuration.HierarchicalConfiguration;

import gjh.bc5.utils.EntityType;
import gjh.bc5.utils.Mention;
import gjh.bc5.utils.Sentence;
import gjh.bc5.utils.Token;

public abstract class Dataset {

	protected Tokenizer tokenizer;
	protected Set<Sentence> sentences;

	protected Dataset() {
		sentences = new TreeSet<Sentence>();
	}

	// TODO This goes away if mentions are character based
	public void setTokenizer(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}

	public abstract void load(HierarchicalConfiguration config);

	public abstract List<Dataset> split(int n);

	public Set<Sentence> getSentences() {
		return Collections.unmodifiableSet(sentences);
	}

	public Map<String, Integer> getTokenCountTotal() {
		Map<String, Integer> counts = new HashMap<String, Integer>();
		for (Sentence sentence : sentences) {
			for (Token token : sentence.getTokens()) {
				String text = token.getText();
				Integer count = counts.get(text);
				if (count == null)
					counts.put(text, 1);
				else
					counts.put(text, count + 1);
			}
		}
		return Collections.unmodifiableMap(counts);
	}

	public Map<EntityType, Integer> getTypeCounts() {
		Map<EntityType, Integer> typeCounts = new HashMap<EntityType, Integer>();
		for (Sentence sentence : sentences) {
			for (Mention mention : sentence.getMentions()) {
				Integer typeCount = typeCounts.get(mention.getEntityType());
				if (typeCount == null)
					typeCounts.put(mention.getEntityType(), new Integer(1));
				else
					typeCounts.put(mention.getEntityType(),
							new Integer(typeCount + 1));
			}
		}
		return typeCounts;
	}

}
