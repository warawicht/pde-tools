package net.jeeeyul.pdetools.clipboard;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.jeeeyul.pdetools.PDEToolsCore;
import net.jeeeyul.pdetools.clipboard.internal.ClipboardPreferenceConstants;
import net.jeeeyul.pdetools.clipboard.internal.ClipboardServiceImpl;
import net.jeeeyul.pdetools.clipboard.internal.ComparatorFactory;
import net.jeeeyul.pdetools.clipboard.internal.SharedColor;
import net.jeeeyul.pdetools.model.pdetools.ClipboardEntry;
import net.jeeeyul.pdetools.shared.ChaindComparator;
import net.jeeeyul.pdetools.shared.UpdateJob;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure0;

public class ClipboardViewer {
	private ComparatorFactory comparatorFactory;
	private SharedColor sharedColor;
	private TableViewer viewer;
	private TableViewerColumn column;
	private IPropertyChangeListener preferenceListener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			handlePreferenceChange(event);
		}
	};

	private EContentAdapter historyListener = new EContentAdapter() {
		public void notifyChanged(Notification notification) {
			if (!notification.isTouch())
				handleNotification(notification);
			super.notifyChanged(notification);
		};
	};
	private int style;
	private ClipEntryLabelProvider labelProvider;
	private UpdateJob updateJob = new UpdateJob(new Procedure0() {
		@Override
		public void apply() {
			if (viewer == null || viewer.getControl().isDisposed()) {
				return;
			}
			viewer.refresh();
		}
	});

	public ClipboardViewer(Composite parent, int style) {
		this.style = style;
		comparatorFactory = new ComparatorFactory();
		
		create(parent);
		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				dispose();
			}
		});

		getPreferenceStore().addPropertyChangeListener(preferenceListener);
	}

	private void create(Composite parent) {
		sharedColor = new SharedColor(parent.getDisplay());
		viewer = new TableViewer(parent, SWT.VIRTUAL | SWT.FULL_SELECTION | style);
		viewer.setUseHashlookup(true);

		column = new TableViewerColumn(viewer, SWT.NORMAL);
		labelProvider = new ClipEntryLabelProvider(sharedColor);
		updateLabelProvider();

		column.setLabelProvider(labelProvider);

		/*
		 * prevent drawing ugly focus
		 */
		viewer.getTable().addListener(SWT.EraseItem, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if ((event.detail & SWT.FOCUSED) != 0) {
					event.detail = event.detail ^ SWT.FOCUSED;
				}
			}
		});

		final Listener resizer = new Listener() {
			@Override
			public void handleEvent(Event event) {
				int size = viewer.getTable().getClientArea().width;
				column.getColumn().setWidth(size);
				viewer.getTable().redraw();
			}
		};
		viewer.getTable().addListener(SWT.Resize, resizer);
		viewer.getTable().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (viewer.getTable().isDisposed()) {
					return;
				}
				resizer.handleEvent(null);
			}
		});

		viewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void dispose() {
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return ClipboardServiceImpl.getInstance().getHistory().getEntries().toArray();
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		
		updateViewerSorter();

		viewer.setInput(ClipboardServiceImpl.getInstance().getHistory());
		ClipboardServiceImpl.getInstance().getHistory().eAdapters().add(historyListener);

		Font font = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry()
				.get("org.eclipse.jface.textfont");
		viewer.getTable().setFont(font);

		DragSource dragSource = new DragSource(viewer.getTable(), DND.DROP_COPY);
		dragSource.setTransfer(new Transfer[] { TextTransfer.getInstance(), FileTransfer.getInstance() });
		dragSource.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragSetData(DragSourceEvent event) {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
					event.data = ((ClipboardEntry) selection.getFirstElement()).getTextContent();
				}

				if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
					String data = ((ClipboardEntry) selection.getFirstElement()).getTextContent();
					try {
						File file = File.createTempFile("clip-board-", ".txt");
						file.deleteOnExit();
						FileWriter writer = new FileWriter(file);
						writer.write(data);
						writer.close();
						event.data = new String[] { file.getAbsolutePath() };
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void dragStart(DragSourceEvent event) {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				if (!selection.isEmpty()) {
					event.detail = DND.DROP_COPY;
					event.doit = true;
				}
			}
		});
	}

	public void dispose() {
		sharedColor.flush();
		ClipboardServiceImpl.getInstance().getHistory().eAdapters().remove(historyListener);
		getPreferenceStore().removePropertyChangeListener(preferenceListener);
	}

	private IPreferenceStore getPreferenceStore() {
		return PDEToolsCore.getDefault().getPreferenceStore();
	}

	public TableViewer getTableViewer() {
		return viewer;
	}

	protected void handleNotification(Notification notification) {
		updateJob.schedule();
	}

	protected void handlePreferenceChange(PropertyChangeEvent event) {
		if (!event.getProperty().startsWith("clipboard-")) {
			return;
		}
		updateViewerSorter();
		updateLabelProvider();
		updateJob.schedule();
	}

	private void updateViewerSorter() {
		final ChaindComparator<ClipboardEntry> comparator = new ChaindComparator<ClipboardEntry>();
		String configString = getPreferenceStore().getString(ClipboardPreferenceConstants.CLIPBOARD_SORT_ORDER);
		for(String each : configString.split(",")){
			comparator.add(comparatorFactory.getByLiteral(each));
		}
		viewer.setSorter(new ViewerSorter(){
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				ClipboardEntry c1 = (ClipboardEntry) e1;
				ClipboardEntry c2 = (ClipboardEntry) e2;
				return comparator.compare(c1, c2);
			}
		});
	}

	public void setFocus() {
	}

	private void updateLabelProvider() {
		labelProvider.setColorizeTextOnSelection(getPreferenceStore().getBoolean(
				ClipboardPreferenceConstants.CLIPBOARD_COLORLIZE_IN_SELECTION));
		labelProvider.setNumberOfLineForRow(getPreferenceStore().getInt(
				ClipboardPreferenceConstants.CLIPBOARD_NUMBER_OF_LINES_PER_EACH_ITEM));
	}
}
