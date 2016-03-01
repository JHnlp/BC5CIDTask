package gjh.bc5.features;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import gjh.bc5.utils.Mention;
import gjh.bc5.utils.Relation;

public class EntityPairIsInstanceSource {

	private Relation relationSource;
	private Set<MentionPairAsInstanceSource> usedMentionPairs;

	public EntityPairIsInstanceSource(Relation relationSource) {
		this.relationSource = relationSource;
		usedMentionPairs = new HashSet<MentionPairAsInstanceSource>();
	}

	public Relation getRelationSource() {
		return relationSource;
	}

	public void setRelationSource(Relation relationSource) {
		this.relationSource = relationSource;
	}

	public Set<MentionPairAsInstanceSource> getUsedMentionPairs() {
		return usedMentionPairs;
	}

	public void setUsedMentionPairs(
			Set<MentionPairAsInstanceSource> mentionPair) {
		this.usedMentionPairs = mentionPair;
	}

	public void addUsedMentionPair(MentionPairAsInstanceSource mp) {
		this.usedMentionPairs.add(mp);
	}

	public void addAllUsedMentionPairs(
			Collection<MentionPairAsInstanceSource> usedMentionPairs) {
		this.usedMentionPairs.addAll(usedMentionPairs);
	}

	public void clearUsedMentionPairs() {
		this.usedMentionPairs.clear();
	}

	public Set<Mention> getUsedChemicalMentions() {
		if (usedMentionPairs == null || usedMentionPairs.isEmpty())
			throw new IllegalStateException("usedMentionPairs are none!");

		Set<Mention> chemMentions = new TreeSet<Mention>();
		for (MentionPairAsInstanceSource mp : usedMentionPairs) {
			chemMentions.add(mp.getChemMention());
		}

		return chemMentions;
	}

	public Set<Mention> getUsedDiseaseMentions() {
		if (usedMentionPairs == null || usedMentionPairs.isEmpty())
			throw new IllegalStateException("usedMentionPairs are none!");

		Set<Mention> disMentions = new TreeSet<Mention>();
		for (MentionPairAsInstanceSource mp : usedMentionPairs) {
			disMentions.add(mp.getDisMention());
		}

		return disMentions;
	}
}
