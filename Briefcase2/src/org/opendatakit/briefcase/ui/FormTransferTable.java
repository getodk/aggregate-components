package org.opendatakit.briefcase.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsSucceededEvent;

public class FormTransferTable extends JTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8511088963758308085L;

	static class FormTransferTableModel extends AbstractTableModel {
	    /**
		 * 
		 */
		private static final long serialVersionUID = 7108326237416622721L;

		private String[] columnNames = { "Selected",
                   "Form Name",
                   "Transfer Status" };

	    final JButton btnSelectOrClearAllForms;
	    final JButton btnTransfer;
	    List<FormStatus> formStatuses = new ArrayList<FormStatus>();
	    
		public FormTransferTableModel(JButton btnSelectOrClearAllForms,
				JButton btnTransfer) {
			AnnotationProcessor.process(this);//if not using AOP

			this.btnSelectOrClearAllForms = btnSelectOrClearAllForms;
			this.btnTransfer = btnTransfer;
			// initially the transfer button is disabled.
			btnTransfer.setEnabled(false);
			btnSelectOrClearAllForms.setText("Clear all");
			
			btnSelectOrClearAllForms.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
		    		boolean anyDeselected = false;
		    		for ( FormStatus f : formStatuses ) {
		    			anyDeselected = anyDeselected || !f.isSelected();
		    		}
		    		
	    			// clear-all if all were selected, otherwise, select-all...
		    		for ( FormStatus f : formStatuses ) {
		    			f.setSelected(anyDeselected);
		    		}
		    		FormTransferTableModel.this.updateButtonsAfterStatusChange();
		    		FormTransferTableModel.this.fireTableDataChanged();
				}});
		}

		private void updateButtonsAfterStatusChange() {
    		boolean anyDeselected = false;
    		boolean anySelected = false;
    		for ( FormStatus f : formStatuses ) {
    			anyDeselected = anyDeselected || !f.isSelected();
    			anySelected = anySelected || f.isSelected();
    		}
			btnTransfer.setEnabled(anySelected);
			if ( !anyDeselected ) {
    			FormTransferTableModel.this.btnSelectOrClearAllForms.setText("Clear all");
			} else {
    			FormTransferTableModel.this.btnSelectOrClearAllForms.setText("Select all");
			}
		}
		
		public void setFormStatusList(List<FormStatus> statuses) {
			formStatuses = statuses;
			updateButtonsAfterStatusChange();
			fireTableDataChanged();
		}
		
		public List<FormStatus> getSelectedForms() {
			List<FormStatus> selected = new ArrayList<FormStatus>();
			for ( FormStatus s : formStatuses ) {
				if ( s.isSelected() ) {
					selected.add(s);
				}
			}
			return selected;
		}

		@Override
		public int getRowCount() {
			return formStatuses.size();
		}
		
        public String getColumnName(int col) {
            return columnNames[col];
        }

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}
        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        @SuppressWarnings({ "rawtypes", "unchecked" })
		public Class getColumnClass(int c) {
        	switch ( c ) {
        	case 0:
        		return Boolean.class;
        	case 1:
        		return String.class;
        	case 2:
        		return String.class;
        	default:
        		throw new IllegalStateException("unexpected column choice");
        	}
        }

        public boolean isCellEditable(int row, int col) {
            return col == 0; // only the checkbox...
        }
        
        public void setValueAt(Object value, int row, int col) {
        	FormStatus status = formStatuses.get(row);
        	switch ( col ) {
        	case 0:
        		status.setSelected((Boolean) value);
    			updateButtonsAfterStatusChange();
        		break;
        	case 2:
        		status.setStatusString((String)value);
        		break;
    		default:
        		throw new IllegalStateException("unexpected column choice");
        	}
            fireTableCellUpdated(row, col);
        }

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
        	FormStatus status = formStatuses.get(rowIndex);
        	switch ( columnIndex ) {
        	case 0:
        		return status.isSelected();
        	case 1:
        		return status.getFormName();
        	case 2:
        		return status.getStatusString();
    		default:
        		throw new IllegalStateException("unexpected column choice");
        	}
		}

		@EventSubscriber(eventClass=FormStatusEvent.class)
		public void fireStatusChange(FormStatusEvent fse) {
			FormStatus fs = fse.getStatus();
			for ( int rowIndex = 0 ; rowIndex < formStatuses.size() ; ++rowIndex ) {
				FormStatus status = formStatuses.get(rowIndex);
				if ( status.equals(fs) ) {
					fireTableRowsUpdated(rowIndex, rowIndex);
					return;
				}
			}
		}
		
	}

	public FormTransferTable(JButton btnSelectOrClearAllForms, JButton btnTransfer) {
		super(new FormTransferTableModel(btnSelectOrClearAllForms, btnTransfer));
		AnnotationProcessor.process(this);//if not using AOP
		TableColumnModel columns = this.getColumnModel();
		// determine width of "Selected" column header
        TableCellRenderer headerRenderer = this.getTableHeader().getDefaultRenderer();
        Component comp = headerRenderer.getTableCellRendererComponent(
                             null, columns.getColumn(0).getHeaderValue(),
                             false, false, 0, 0);
        int headerWidth = comp.getPreferredSize().width;
        columns.getColumn(0).setMinWidth(headerWidth);
        columns.getColumn(0).setMaxWidth(headerWidth);
        columns.getColumn(0).setPreferredWidth(headerWidth);

        // and scale the others to be wider...
        columns.getColumn(1).setPreferredWidth(10*headerWidth);
		columns.getColumn(2).setPreferredWidth(10*headerWidth);
		this.setFillsViewportHeight(true);
	}
	
	public void setFormStatusList(List<FormStatus> statuses) {
		FormTransferTableModel model = (FormTransferTableModel) this.dataModel;
		model.setFormStatusList(statuses);
	}

	@EventSubscriber(eventClass=RetrieveAvailableFormsSucceededEvent.class)
	public void formsAvailableFromServer(RetrieveAvailableFormsSucceededEvent event) {
		setFormStatusList(event.getFormsToTransfer());
	}
	
	public List<FormStatus> getSelectedForms() {
		FormTransferTableModel model = (FormTransferTableModel) this.dataModel;
		return model.getSelectedForms();
	}
}
