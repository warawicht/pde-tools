package net.jeeeyul.pdetools.shell

import java.io.File
import net.jeeeyul.pdetools.shared.LaunchCommand
import net.jeeeyul.pdetools.shared.SharedImages

class ShowInShellLinuxHandler extends AbstractShowInShellHandler {

	override createLaunchCommand(File targetFile) {
		new LaunchCommand() => [
			var folder = targetFile
			if(!targetFile.directory){
				folder = targetFile.parentFile
			}
			
			commands += "/bin/bash"
			commands += "-c"
			commands += '''xdg-open "«folder.absolutePath»"'''.toString
		]
	}
	
	override protected getLabel() {
		
	}
	
	override protected getImageDescriptor() {
		SharedImages::getImageDescriptor(SharedImages::NAUTILUS)
	}
	
}