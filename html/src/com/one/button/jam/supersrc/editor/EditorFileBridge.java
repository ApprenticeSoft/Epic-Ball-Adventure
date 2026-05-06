package editor;

public final class EditorFileBridge {
	private EditorFileBridge(){
	}

	public static native void saveText(String fileName, String content) /*-{
		var safeName = fileName || "Editor Level.tmx";
		if(!/\.tmx$/i.test(safeName))
			safeName += ".tmx";
		safeName = safeName.replace(/[\\\/]/g, "_");
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
}
