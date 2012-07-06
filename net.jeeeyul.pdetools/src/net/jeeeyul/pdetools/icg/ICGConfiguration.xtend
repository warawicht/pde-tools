package net.jeeeyul.pdetools.icg

import net.jeeeyul.pdetools.Activator
import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ProjectScope
import org.eclipse.core.runtime.Assert
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.ui.preferences.ScopedPreferenceStore

import static net.jeeeyul.pdetools.icg.ICGConfiguration.*

class ICGConfiguration {
	private static val MONITORING_FOLDER = "monitoring-folder";
	private static val GENERATE_SRC_FOLDER  = "generate-src-folder";
	private static val GENERATE_PACKAGE  = "generate-package";
	private static val GENERATE_CLASS  = "generate-class";
	private static val MARK_DEREIVED = "mark-derived";
	private static val IMAGE_FILE_EXTENSIONS = "image-file-extensions";
	
	IProject project;
	ScopedPreferenceStore _store;
	
	new(IProject project){
		Assert::isNotNull(project);
		this.project = project
	}
	
	def IFolder getGenerateSrcFolder(){
		store.getFolder(GENERATE_SRC_FOLDER)
	}
	
	def void setGenerateSrcFolder(IFolder folder){
		store.setValue(GENERATE_SRC_FOLDER, folder)
	}
	
	def String getGeneratePackageName(){
		store.getString(GENERATE_PACKAGE);
	}
	
	def void setGeneratePackageName(String packageName){
		store.setValue(GENERATE_PACKAGE, packageName)
	}
	
	def getGenerateClassName(){
		store.getString(GENERATE_CLASS)
	}
	
	def void setGenerateClassName(String className){
		store.setValue(GENERATE_CLASS, className)
	}
	
	def IFolder getMonitoringFolder(){
		store.getFolder(MONITORING_FOLDER)
	}
	
	def void setMonitoringFolder(IFolder folder){
		store.setValue(MONITORING_FOLDER,  folder)
	}
	
	def isMarkDerived(){
		store.getBoolean(MARK_DEREIVED)
	}
	
	def setMarkDerived(boolean markDerived){
		store.setValue(MARK_DEREIVED, markDerived)
	}
	
	def String[] getImageFileExtensions(){
		var expression = store.getString(IMAGE_FILE_EXTENSIONS)
		
		if(expression == null){
			return emptyList
		}
		
		return expression.split("[ ,]+").map[it.trim].filter[length>0]
	}
	
	def void setImageFileExtensions(String[] extensions){
		store.setValue(IMAGE_FILE_EXTENSIONS, extensions.join(", "))
	}
	
	def private  store(){
		if(_store == null){
			_store = new ScopedPreferenceStore(new ProjectScope(project), Activator::PLUGIN_ID + ".icg"); 
		}
		return _store;
	}
	
	def private getFolder(IPreferenceStore store, String key){
		var value = store.getString(key);
		if(value.nullOrEmpty){
			return null;
		}
		project.getFolder(value)
	}
	
	def private setValue(IPreferenceStore store, String key, IFolder folder){
		if(folder == null){
			store.setValue(key, "")
		}else{
			store.setValue(key, folder.projectRelativePath.toPortableString)	
		}
	}
	
	def save(){
		store.save();
	}
}