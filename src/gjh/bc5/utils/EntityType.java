package gjh.bc5.utils;

public class EntityType extends SerialCloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3229545633735309610L;
	private String typeName;

	public EntityType() {
		typeName = "";
	}

	public EntityType(String typeName) {
		this.typeName = typeName;
	}

	// deep copy
	public EntityType(EntityType e) {
		this.typeName = e.getTypeName();
	}

	// public EntityType clone() throws CloneNotSupportedException {
	// EntityType cloned = (EntityType) super.clone();
	// return cloned;
	// }

	public String getTypeName() {
		return typeName;
	}

	public void setText(String typeName) {
		this.typeName = typeName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((typeName == null) ? 0 : typeName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityType other = (EntityType) obj;
		if (typeName == null) {
			if (other.typeName != null)
				return false;
		} else if (!typeName.equals(other.typeName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EntityType [typeName=" + typeName + "]";
	}

}
