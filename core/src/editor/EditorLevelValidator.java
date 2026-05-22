package editor;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public final class EditorLevelValidator {
	private static final float BOUNDS_EPSILON = 0.5f;

	private EditorLevelValidator(){
	}

	public static Array<String> validate(EditorLevel level){
		Array<String> errors = new Array<String>();
		validateWorld(level, errors);
		validateRequiredObjects(level, errors);
		validateObjects(level, errors);
		validatePulleyPairs(level, errors);
		return errors;
	}

	private static void validateWorld(EditorLevel level, Array<String> errors){
		if(level.widthTiles < 1)
			errors.add("World width must be at least 1 tile");
		if(level.heightTiles < 1)
			errors.add("World height must be at least 1 tile");
	}

	private static void validateRequiredObjects(EditorLevel level, Array<String> errors){
		int starts = 0;
		int exits = 0;
		for(EditorLevelObject object : level.objects){
			if(object.type == EditorObjectType.START)
				starts++;
			else if(object.type == EditorObjectType.EXIT)
				exits++;
		}
		if(starts == 0)
			errors.add("Missing Start object");
		else if(starts > 1)
			errors.add("Only one Start object is allowed");
		if(exits == 0)
			errors.add("Missing Exit object");
	}

	private static void validateObjects(EditorLevel level, Array<String> errors){
		float worldWidth = level.getPixelWidth();
		float worldHeight = level.getPixelHeight();
		for(EditorLevelObject object : level.objects){
			validateBounds(object, worldWidth, worldHeight, errors);
			validateGeometry(object, errors);
			validateProperties(object, errors);
		}
	}

	private static void validateBounds(EditorLevelObject object, float worldWidth, float worldHeight, Array<String> errors){
		if(object.getMinX() < -BOUNDS_EPSILON || object.getMinY() < -BOUNDS_EPSILON
				|| object.getMaxX() > worldWidth + BOUNDS_EPSILON || object.getMaxY() > worldHeight + BOUNDS_EPSILON)
			errors.add(object.type.label + " is outside world bounds");
	}

	private static void validateGeometry(EditorLevelObject object, Array<String> errors){
		if(object.type == EditorObjectType.POLYGON && object.points.size < 3)
			errors.add("Polygon needs at least 3 points");
		else if(object.type == EditorObjectType.PLATFORM && object.points.size < 2)
			errors.add("Moving platform needs at least 2 path points");
		else if(!object.usesPointGeometry() && (object.width <= 0f || object.height <= 0f))
			errors.add(object.type.label + " must have positive width and height");
	}

	private static void validateProperties(EditorLevelObject object, Array<String> errors){
		if(object.unsupportedTmxType != null)
			errors.add("Unsupported object type would be dropped: " + object.unsupportedTmxType);
		String[] numericNames = numericPropertyNames(object.type);
		for(String propertyName : numericNames){
			String value = object.properties.get(propertyName);
			if(value == null || value.trim().length() == 0)
				continue;
			try{
				float parsed = Float.parseFloat(value);
				if(Float.isNaN(parsed) || Float.isInfinite(parsed)){
					errors.add(object.type.label + " " + propertyName + " must be numeric");
					continue;
				}
				if(requiresPositiveValue(propertyName) && parsed <= 0f)
					errors.add(object.type.label + " " + propertyName + " must be positive");
				if("Groupe".equals(propertyName) && Math.abs(parsed - Math.round(parsed)) > 0.0001f)
					errors.add("Poulie Groupe must be an integer");
				if("Groupe".equals(propertyName) && parsed <= 0f)
					errors.add("Poulie Groupe must be positive");
			}
			catch(NumberFormatException exception){
				errors.add(object.type.label + " " + propertyName + " must be numeric");
			}
		}
		validateBooleanProperties(object, errors);
		if(object.type == EditorObjectType.POULIE && object.properties.get("Groupe") == null)
			errors.add("Poulie is missing Groupe");
	}

	private static void validateBooleanProperties(EditorLevelObject object, Array<String> errors){
		if(object.type == EditorObjectType.PLATFORM)
			validateBooleanProperty(object, "Loop", true, errors);
		else if(object.type == EditorObjectType.SWING)
			validateBooleanProperty(object, "Contact", false, errors);
	}

	private static void validateBooleanProperty(EditorLevelObject object, String propertyName, boolean allowPresenceOnly,
			Array<String> errors){
		String value = object.properties.get(propertyName);
		if(value == null)
			return;
		String trimmed = value.trim();
		if(trimmed.length() == 0 && allowPresenceOnly)
			return;
		if(isBooleanValue(trimmed))
			return;
		errors.add(object.type.label + " " + propertyName + " must be true or false");
	}

	private static void validatePulleyPairs(EditorLevel level, Array<String> errors){
		ObjectMap<String, Integer> groupCounts = new ObjectMap<String, Integer>();
		for(EditorLevelObject object : level.objects){
			if(object.type != EditorObjectType.POULIE)
				continue;
			String group = object.properties.get("Groupe");
			if(group == null)
				continue;
			Integer count = groupCounts.get(group);
			groupCounts.put(group, count == null ? 1 : count + 1);
		}
		for(ObjectMap.Entry<String, Integer> entry : groupCounts){
			if(entry.value != 2)
				errors.add("Poulie group " + entry.key + " must contain exactly 2 objects");
		}
	}

	private static String[] numericPropertyNames(EditorObjectType type){
		if(type == EditorObjectType.LIGHT)
			return new String[]{"Weight"};
		if(type == EditorObjectType.REVOLVING)
			return new String[]{"Speed"};
		if(type == EditorObjectType.SWING)
			return new String[]{"Position", "Weight", "Speed", "Torque", "angleRef", "angleMin", "angleMax"};
		if(type == EditorObjectType.BALANCOIRE)
			return new String[]{"AttacheY", "Weight"};
		if(type == EditorObjectType.SUSPENDU)
			return new String[]{"Length", "Position", "Weight"};
		if(type == EditorObjectType.POULIE)
			return new String[]{"Groupe", "Masse", "longueur"};
		if(type == EditorObjectType.SPRING)
			return new String[]{"PowerX", "PowerY"};
		if(type == EditorObjectType.PLATFORM)
			return new String[]{"Speed", "Width"};
		return new String[0];
	}

	private static boolean requiresPositiveValue(String propertyName){
		return "Weight".equals(propertyName) || "Masse".equals(propertyName) || "longueur".equals(propertyName)
				|| "Length".equals(propertyName) || "Width".equals(propertyName) || "Torque".equals(propertyName);
	}

	private static boolean isBooleanValue(String value){
		return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) || "oui".equalsIgnoreCase(value)
				|| "non".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value)
				|| "1".equals(value) || "0".equals(value);
	}
}
