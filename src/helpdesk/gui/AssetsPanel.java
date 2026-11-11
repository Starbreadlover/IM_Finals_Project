package helpdesk.gui;

import helpdesk.dao.AssetDao;
import helpdesk.model.Asset;
import helpdesk.model.AssetStatus;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;

/** Assets tab: list, add, edit and delete. */
class AssetsPanel extends JPanel implements Refreshable {

    private final AssetDao assetDao = new AssetDao();
    private final DefaultTableModel model = Ui.tableModel(
            "ID", "Tag", "Type", "Brand", "Model", "Serial no.", "Location", "Status");
    private final JTable table = Ui.table(model);

    AssetsPanel() {
        super(new BorderLayout());
        add(Ui.buttonBar(
                Ui.button("Refresh", this::refresh),
                Ui.button("Add asset", () -> showAssetForm(null)),
                Ui.button("Edit asset", this::edit),
                Ui.button("Delete asset", this::delete)), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
    }

    @Override
    public void refresh() {
        try {
            model.setRowCount(0);
            for (Asset a : assetDao.findAll()) {
                model.addRow(new Object[]{a.id(), a.assetTag(), a.assetType(), Ui.orEmpty(a.brand()),
                        Ui.orEmpty(a.model()), Ui.orEmpty(a.serialNumber()), Ui.orEmpty(a.location()),
                        a.status().label()});
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private Optional<Asset> selectedAsset() throws SQLException {
        Integer id = Ui.selectedId(table);
        if (id == null) {
            Ui.warn(this, "Select an asset in the table first.");
            return Optional.empty();
        }
        return assetDao.findById(id);
    }

    private void edit() {
        try {
            selectedAsset().ifPresent(this::showAssetForm);
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    /** Add form when current is null, edit form otherwise. */
    private void showAssetForm(Asset current) {
        JTextField tag = new JTextField(current == null ? "" : current.assetTag(), 25);
        JTextField type = new JTextField(current == null ? "" : current.assetType(), 25);
        JTextField brand = new JTextField(current == null ? "" : Ui.orEmpty(current.brand()), 25);
        JTextField modelName = new JTextField(current == null ? "" : Ui.orEmpty(current.model()), 25);
        JTextField serial = new JTextField(current == null ? "" : Ui.orEmpty(current.serialNumber()), 25);
        JTextField location = new JTextField(current == null ? "" : Ui.orEmpty(current.location()), 25);
        JComboBox<AssetStatus> status = new JComboBox<>(AssetStatus.values());
        if (current != null) {
            status.setSelectedItem(current.status());
        }
        JPanel form = Ui.form("Asset tag", tag, "Type (e.g. Laptop)", type, "Brand", brand, "Model", modelName,
                "Serial number", serial, "Location", location, "Status", status);
        String title = current == null ? "Add asset" : "Edit asset " + current.assetTag();

        while (Ui.showForm(this, title, form)) {
            if (tag.getText().isBlank() || type.getText().isBlank()) {
                Ui.warn(this, "Asset tag and type are required.");
                continue;
            }
            Asset asset = new Asset(current == null ? 0 : current.id(), tag.getText().trim(), type.getText().trim(),
                    brand.getText().trim(), modelName.getText().trim(), serial.getText().trim(),
                    location.getText().trim(), (AssetStatus) status.getSelectedItem());
            try {
                if (current == null) {
                    int id = assetDao.insert(asset);
                    Ui.info(this, "Asset #" + id + " added.");
                } else {
                    assetDao.update(asset);
                    Ui.info(this, "Asset updated.");
                }
                refresh();
                return;
            } catch (SQLIntegrityConstraintViolationException e) {
                Ui.warn(this, "Asset tag " + asset.assetTag() + " already exists.");
            } catch (SQLException e) {
                Ui.dbError(this, e);
                return;
            }
        }
    }

    private void delete() {
        try {
            Optional<Asset> found = selectedAsset();
            if (found.isEmpty() || !Ui.confirm(this, "Delete asset " + found.get().assetTag() + "?")) {
                return;
            }
            try {
                assetDao.delete(found.get().id());
                refresh();
                Ui.info(this, "Asset deleted.");
            } catch (SQLIntegrityConstraintViolationException e) {
                Ui.warn(this, "This asset is linked to a ticket, so it cannot be deleted.\n"
                        + "Edit it and set its status to Retired instead.");
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }
}
