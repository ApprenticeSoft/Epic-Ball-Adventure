package editor;

public final class EditorFileBridge {
	private EditorFileBridge(){
	}

	public static String loadText(String fileName){
		String sanitized = sanitize(fileName);
		String text = loadStoredText(sanitized);
		if(text != null)
			return text;
		text = loadBundledText(sanitized);
		if(text != null)
			return text;
		throw new RuntimeException("Level not found: " + sanitized);
	}

	public static native void saveText(String fileName, String content) /*-{
		var safeName = @editor.EditorFileBridge::sanitize(Ljava/lang/String;)(fileName);
		try{
			if($wnd.localStorage)
				$wnd.localStorage.setItem("epicBallEditor:" + safeName, content);
		}
		catch(e){
		}
		var blob = new Blob([content], {type: "application/xml;charset=utf-8"});
		var url = URL.createObjectURL(blob);
		var link = $doc.createElement("a");
		link.href = url;
		link.download = safeName;
		$doc.body.appendChild(link);
		link.click();
		$doc.body.removeChild(link);
		URL.revokeObjectURL(url);
	}-*/;

	public static native String saveTextWithBackup(String fileName, String content) /*-{
		var safeName = @editor.EditorFileBridge::sanitize(Ljava/lang/String;)(fileName);
		var backupName = null;
		try{
			if($wnd.localStorage){
				var key = "epicBallEditor:" + safeName;
				var previous = $wnd.localStorage.getItem(key);
				if(previous !== null){
					var baseName = /\.tmx$/i.test(safeName) ? safeName.substring(0, safeName.length - 4) : safeName;
					backupName = baseName + "." + (new Date()).getTime() + ".bak.tmx";
					$wnd.localStorage.setItem("epicBallEditorBackup:" + backupName, previous);
				}
			}
		}
		catch(e){
			backupName = null;
		}
		@editor.EditorFileBridge::saveText(Ljava/lang/String;Ljava/lang/String;)(safeName, content);
		return backupName;
	}-*/;

	public static String sanitize(String fileName){
		String value = fileName == null || fileName.trim().length() == 0 ? "Editor Level.tmx" : fileName.trim();
		if(!value.endsWith(".tmx"))
			value += ".tmx";
		return value.replace('\\', '_').replace('/', '_');
	}

	private static native String loadStoredText(String safeName) /*-{
		try{
			if($wnd.localStorage)
				return $wnd.localStorage.getItem("epicBallEditor:" + safeName);
		}
		catch(e){
		}
		return null;
	}-*/;

	private static native String loadBundledText(String safeName) /*-{
		var assetPath = "Levels/" + safeName;
		var hashedPath = null;
		try{
			var manifest = new XMLHttpRequest();
			manifest.open("GET", "assets/assets.txt", false);
			manifest.send(null);
			if(manifest.status >= 200 && manifest.status < 300){
				var lines = manifest.responseText.split(/\r?\n/);
				for(var i = 0; i < lines.length; i++){
					var parts = lines[i].split(":");
					if(parts.length >= 3 && parts[1] === assetPath){
						hashedPath = parts[2];
						break;
					}
				}
			}
		}
		catch(e){
		}
		var paths = [];
		if(hashedPath)
			paths.push("assets/" + hashedPath);
		paths.push("assets/" + assetPath);
		for(var j = 0; j < paths.length; j++){
			try{
				var request = new XMLHttpRequest();
				request.open("GET", paths[j], false);
				request.send(null);
				if(request.status >= 200 && request.status < 300)
					return request.responseText;
			}
			catch(e){
			}
		}
		return null;
	}-*/;
}
