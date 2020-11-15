package com.github.stefvanschie.inventoryframework.gui.type;

import com.github.stefvanschie.inventoryframework.exception.XMLLoadException;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.util.NamedGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a gui in the form of an ender chest
 *
 * @since 0.8.0
 */
public class EnderChestGui extends NamedGui {

    /**
     * Represents the inventory component for the entire gui
     */
    @NotNull
    private InventoryComponent inventoryComponent = new InventoryComponent(9, 7);

    /**
     * Constructs a new GUI
     *
     * @param title the title/name of this gui.
     * @since 0.8.0
     */
    public EnderChestGui(@NotNull String title) {
        super(title);
    }

    @Override
    public void show(@NotNull HumanEntity humanEntity) {
        getInventory().clear();

        getHumanEntityCache().storeAndClear(humanEntity);

        int height = getInventoryComponent().getHeight();

        getInventoryComponent().display();

        InventoryComponent topComponent = getInventoryComponent().excludeRows(height - 4, height - 1);
        InventoryComponent bottomComponent = getInventoryComponent().excludeRows(0, height - 5);

        topComponent.placeItems(getInventory(), 0);
        bottomComponent.placeItems(humanEntity.getInventory(), 0);

        if (!bottomComponent.hasItem()) {
            getHumanEntityCache().restoreAndForget(humanEntity);
        }

        humanEntity.openInventory(getInventory());
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public EnderChestGui copy() {
        EnderChestGui gui = new EnderChestGui(getTitle());

        gui.inventoryComponent = inventoryComponent.copy();

        gui.setOnTopClick(this.onTopClick);
        gui.setOnBottomClick(this.onBottomClick);
        gui.setOnGlobalClick(this.onGlobalClick);
        gui.setOnOutsideClick(this.onOutsideClick);
        gui.setOnClose(this.onClose);

        return gui;
    }

    @Contract(pure = true)
    @Override
    public boolean isPlayerInventoryUsed() {
        return getInventoryComponent().excludeRows(0, getInventoryComponent().getHeight() - 5).hasItem();
    }

    @Override
    public void click(@NotNull InventoryClickEvent event) {
        getInventoryComponent().click(this, event, event.getRawSlot());
    }

    /**
     * Adds a pane to this gui
     *
     * @param pane the pane to add
     * @since 0.8.0
     */
    public void addPane(@NotNull Pane pane) {
        this.inventoryComponent.addPane(pane);
    }

    /**
     * Gets all the panes in this gui, this includes child panes from other panes
     *
     * @return all panes
     */
    @NotNull
    @Contract(pure = true)
    public List<Pane> getPanes() {
        return this.inventoryComponent.getPanes();
    }

    /**
     * Gets all the items in all underlying panes
     *
     * @return all items
     */
    @NotNull
    @Contract(pure = true)
    public Collection<GuiItem> getItems() {
        return getPanes().stream().flatMap(pane -> pane.getItems().stream()).collect(Collectors.toSet());
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public Inventory createInventory(@NotNull String title) {
        return Bukkit.createInventory(this, InventoryType.ENDER_CHEST, title);
    }

    /**
     * Gets the inventory component for this gui
     *
     * @return the inventory component
     * @since 0.8.0
     */
    @NotNull
    @Contract(pure = true)
    public InventoryComponent getInventoryComponent() {
        return inventoryComponent;
    }

    /**
     * Loads an ender chest gui from an XML file.
     *
     * @param instance the instance on which to reference fields and methods
     * @param inputStream the input stream containing the XML data
     * @return the loaded ender chest gui
     * @since 0.8.0
     */
    @Nullable
    @Contract(pure = true)
    public static EnderChestGui load(@NotNull Object instance, @NotNull InputStream inputStream) {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
            Element documentElement = document.getDocumentElement();

            documentElement.normalize();

            return load(instance, documentElement);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads an ender chest gui from the specified element, applying code references to the provided instance.
     *
     * @param instance the instance on which to reference fields and methods
     * @param element the element to load the gui from
     * @return the loaded ender chest gui
     * @since 0.8.0
     */
    @NotNull
    public static EnderChestGui load(@NotNull Object instance, @NotNull Element element) {
        if (!element.hasAttribute("title")) {
            throw new XMLLoadException("Provided XML element's gui tag doesn't have the mandatory title attribute set");
        }

        EnderChestGui enderChestGui = new EnderChestGui(element.getAttribute("title"));
        enderChestGui.initializeOrThrow(instance, element);

        if (element.hasAttribute("populate")) {
            return enderChestGui;
        }

        NodeList childNodes = element.getChildNodes();

        for (int index = 0; index < childNodes.getLength(); index++) {
            Node item = childNodes.item(index);

            if (item.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element componentElement = (Element) item;
            InventoryComponent inventoryComponent = enderChestGui.getInventoryComponent();

            if (componentElement.getTagName().equalsIgnoreCase("component")) {
                inventoryComponent.load(instance, componentElement);
            } else {
                inventoryComponent.load(instance, element);
            }

            break;
        }

        return enderChestGui;
    }
}
