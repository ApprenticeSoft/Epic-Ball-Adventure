package editor;

public enum EditorObjectType {
	START("Start", "Ball", false),
	SOLID("Solid", null, false),
	POLYGON("Polygon", null, true),
	LIGHT("Light", "Light", false),
	REVOLVING("Revolving", "Revolving", false),
	SWING("Swing", "Swing", false),
	BALANCOIRE("Balancoire", "Balancoire", false),
	SUSPENDU("Suspendu", "Suspendu", false),
	POULIE("Poulie", "Poulie", false),
	WATER("Water", "Water", false),
	SPRING("Spring", "Spring", false),
	EXIT("Exit", "Exit", false),
	PLATFORM("Platform", null, false);

	public final String label;
	public final String tmxType;
	public final boolean polygon;

	EditorObjectType(String label, String tmxType, boolean polygon){
		this.label = label;
		this.tmxType = tmxType;
		this.polygon = polygon;
	}
}
