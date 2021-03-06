package jp.primecloud.auto.ui;

import java.util.List;

import jp.primecloud.auto.entity.crud.ComponentType;
import jp.primecloud.auto.exception.AutoApplicationException;
import jp.primecloud.auto.service.ComponentService;
import jp.primecloud.auto.service.LoadBalancerService;
import jp.primecloud.auto.service.dto.ComponentDto;
import jp.primecloud.auto.service.dto.LoadBalancerPlatformDto;
import jp.primecloud.auto.ui.util.BeanContext;
import jp.primecloud.auto.ui.util.Icons;
import jp.primecloud.auto.ui.util.VaadinUtils;
import jp.primecloud.auto.ui.util.ViewContext;
import jp.primecloud.auto.ui.util.ViewMessages;
import jp.primecloud.auto.ui.util.ViewProperties;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import com.vaadin.Application;
import com.vaadin.data.Property;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * <p>
 * ロードバランサ新規作成画面を生成します。
 * </p>
 *
 */
@SuppressWarnings("serial")
public class WinLoadBalancerAdd extends Window {
    final String COLUMN_HEIGHT = "30px";

    Application apl;

    TextField loadBalancerNameField;

    TextField commentField;

    SelectCloudTable cloudTable;

    SelectTypeTable typeTable;

    SelectServiceTable serviceTable;

    List<LoadBalancerPlatformDto> platforms;

    List<ComponentDto> componentDtos;

    WinLoadBalancerAdd(Application ap) {
        apl = ap;

        //モーダルウインドウ
        setIcon(Icons.ADD.resource());
        setCaption(ViewProperties.getCaption("window.winLoadBalancerAdd"));
        setModal(true);
        setWidth("600px");
        //setHeight("500px");

        VerticalLayout layout = (VerticalLayout) getContent();
        layout.setMargin(false, true, false, true);
        layout.setSpacing(false);

        // フォーム
        layout.addComponent(new BasicForm());

        // 下部のバー
        HorizontalLayout okbar = new HorizontalLayout();
        okbar.setSpacing(true);
        okbar.setMargin(false, false, true, false);
        layout.addComponent(okbar);
        layout.setComponentAlignment(okbar, Alignment.BOTTOM_RIGHT);

        // AddButtonボタン
        Button addButton = new Button();
        addButton.setCaption(ViewProperties.getCaption("button.add"));
        addButton.setDescription(ViewProperties.getCaption("description.addLoadBalancer"));

        addButton.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                addButtonClick(event);
            }
        });
        // [Enter]でaddButtonクリック
        addButton.setClickShortcut(KeyCode.ENTER);
        addButton.focus();
        okbar.addComponent(addButton);

        // Cancel
        Button cancelButton = new Button(ViewProperties.getCaption("button.cancel"));
        cancelButton.setDescription(ViewProperties.getCaption("description.cancel"));
        cancelButton.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                close();
            }
        });
        okbar.addComponent(cancelButton);

        // 入力チェックの設定
        initValidation();

        // 初期データの取得
        initData();

        // データの表示
        showData();
    }

    private class BasicForm extends Form {
        BasicForm() {
            // LB名
            loadBalancerNameField = new TextField(ViewProperties.getCaption("field.loadBalancerName"));
            getLayout().addComponent(loadBalancerNameField);

            // コメント欄
            commentField = new TextField(ViewProperties.getCaption("field.comment"));
            commentField.setWidth("95%");
            getLayout().addComponent(commentField);

            // クラウド選択
            cloudTable = new SelectCloudTable();
            getLayout().addComponent(cloudTable);

            // LB種別選択
            typeTable = new SelectTypeTable();
            getLayout().addComponent(typeTable);

            // 割り当てサービス選択
            serviceTable = new SelectServiceTable();
            getLayout().addComponent(serviceTable);
        }
    }

    private class SelectCloudTable extends Table {
        SelectCloudTable() {
            //テーブル基本設定
            setCaption(ViewProperties.getCaption("table.selectCloud"));
            setWidth("340px");
            setPageLength(3);
            setColumnHeaderMode(Table.COLUMN_HEADER_MODE_HIDDEN);
            setSortDisabled(true);
            setColumnReorderingAllowed(false);
            setColumnCollapsingAllowed(false);
            setSelectable(true);
            setMultiSelect(false);
            setNullSelectionAllowed(false);
            setImmediate(true);
            //            addStyleName("win-server-add-cloud");
            addStyleName("loadbalancer-add-table");

            //カラム設定
            addContainerProperty("No", Integer.class, null);
            addContainerProperty("Cloud", Label.class, new Label());
            setColumnExpandRatio("Cloud", 100);

            //テーブルのカラムに対してStyleNameを設定
            setCellStyleGenerator(new Table.CellStyleGenerator() {
                public String getStyle(Object itemId, Object propertyId) {
                    if (propertyId == null) {
                        return "";
                    } else {
                        return propertyId.toString().toLowerCase();
                    }
                }
            });

            // 行が選択されたときのイベント
            addListener(new Property.ValueChangeListener() {
                @Override
                public void valueChange(Property.ValueChangeEvent event) {
                    Long platformNo = (Long) getValue();

                    // ロードバランサーTypeを表示する
                    showTypes(platformNo);
                }
            });
        }
    }

    private class SelectTypeTable extends Table {

        SelectTypeTable() {
            //テーブル基本設定
            setCaption(ViewProperties.getCaption("table.loadBalancerType"));
            //            setWidth("100%");
            setWidth("340px");
            setPageLength(2);
            setColumnHeaderMode(Table.COLUMN_HEADER_MODE_HIDDEN);
            setSortDisabled(true);
            setColumnReorderingAllowed(false);
            setColumnCollapsingAllowed(false);
            setSelectable(true);
            setMultiSelect(false);
            setNullSelectionAllowed(false);
            setImmediate(true);
            addStyleName("loadbalancer-add-table");

            //カラム設定
            addContainerProperty("No", Integer.class, null);
            addContainerProperty("Detail", Label.class, new Label());
            setColumnExpandRatio("Detail", 100);

            //テーブルのカラムに対してStyleNameを設定
            setCellStyleGenerator(new Table.CellStyleGenerator() {
                public String getStyle(Object itemId, Object propertyId) {
                    if (propertyId == null) {
                        return "";
                    } else {
                        return propertyId.toString().toLowerCase();
                    }
                }
            });
        }
    }

    //サービス選択テーブル
    private class SelectServiceTable extends Table {

        SelectServiceTable() {
            //テーブル基本設定
            setCaption(ViewProperties.getCaption("table.loadBalancerService"));
            //            setWidth("340px");
            setWidth("98%");
            setPageLength(4);
            setColumnHeaderMode(Table.COLUMN_HEADER_MODE_HIDDEN);
            setSortDisabled(true);
            setColumnReorderingAllowed(false);
            setColumnCollapsingAllowed(false);
            setSelectable(true);
            setMultiSelect(false);
            setNullSelectionAllowed(false);
            setImmediate(true);
            addStyleName("loadbalancer-select-service");

            //カラム設定
            //            addContainerProperty("No", Integer.class, null);
            //            addContainerProperty("Name", String.class, null);
            addContainerProperty("Name", Label.class, new Label());
            addContainerProperty("Detail", Label.class, new Label());
            setColumnExpandRatio("Detail", 100);

            //テーブルのカラムに対してStyleNameを設定
            setCellStyleGenerator(new Table.CellStyleGenerator() {
                public String getStyle(Object itemId, Object propertyId) {
                    if (propertyId == null) {
                        return "";
                    } else {
                        return propertyId.toString().toLowerCase();
                    }
                }
            });
        }
    }

    private void initData() {
        Long userNo = ViewContext.getUserNo();
        Long farmNo = ViewContext.getFarmNo();

        // クラウド情報を取得
        LoadBalancerService loadBalancerService = BeanContext.getBean(LoadBalancerService.class);
        platforms = loadBalancerService.getPlatforms(userNo);

        // サービス情報を取得
        ComponentService componentService = BeanContext.getBean(ComponentService.class);
        componentDtos = componentService.getComponents(farmNo);
    }

    private void initValidation() {
        String message = ViewMessages.getMessage("IUI-000055");
        loadBalancerNameField.setRequired(true);
        loadBalancerNameField.setRequiredError(message);
        loadBalancerNameField.addValidator(new StringLengthValidator(message, 1, 15, false));
        loadBalancerNameField.addValidator(new RegexpValidator("^[0-9a-z]|[0-9a-z][0-9a-z-]*[0-9a-z]$", true, message));

        message = ViewMessages.getMessage("IUI-000003");
        commentField.addValidator(new StringLengthValidator(message, -1, 100, true));
    }

    private void showData() {
        // クラウドテーブルを表示
        showClouds();

        // 割り当てサービステーブルを表示
        showServices();
    }

    private void showClouds() {
        cloudTable.removeAllItems();

        // クラウド情報をテーブルに追加
        for (int i = 0; i < platforms.size(); i++) {
            LoadBalancerPlatformDto platformDto = platforms.get(i);

            if (BooleanUtils.isNotTrue(platformDto.getPlatform().getSelectable())) {
                //使用不可プラットフォームの場合、非表示
                continue;
            }

            // TODO: アイコン名の取得ロジックのリファクタリング
            Icons icon = Icons.NONE;
            if ("aws".equals(platformDto.getPlatform().getPlatformType())) {
                if (platformDto.getPlatformAws().getEuca()) {
                    icon = Icons.EUCALYPTUS;
                } else {
                    icon = Icons.AWS;
                }
            } else if ("vmware".equals(platformDto.getPlatform().getPlatformType())) {
                icon = Icons.VMWARE;
            } else if ("nifty".equals(platformDto.getPlatform().getPlatformType())) {
                icon = Icons.NIFTY;
            } else if ("cloudstack".equals(platformDto.getPlatform().getPlatformType())){
                icon = Icons.CLOUD_STACK;
            }

            String description = platformDto.getPlatform().getPlatformNameDisp();

            Label slbl = new Label("<img src=\"" + VaadinUtils.getIconPath(apl, icon) + "\"><div>" + description
                    + "</div>", Label.CONTENT_XHTML);
            slbl.setHeight(COLUMN_HEIGHT);

            cloudTable.addItem(new Object[] { (i + 1), slbl }, platformDto.getPlatform().getPlatformNo());
        }

        Long platformNo = null;
        if (cloudTable.getItemIds().size() > 0) {
            platformNo = (Long) cloudTable.getItemIds().toArray()[0];
        }

        // 先頭のクラウド情報を選択する
        cloudTable.select(platformNo);
    }

    private void showTypes(Long platformNo) {
        typeTable.removeAllItems();
        if (platformNo == null) {
            return;
        }

        // 選択されたクラウドで利用可能なロードバランサ種別情報を取得
        List<String> types = null;
        for (LoadBalancerPlatformDto platform : platforms) {
            if (platformNo.equals(platform.getPlatform().getPlatformNo())) {
                types = platform.getTypes();
                break;
            }
        }

        // ロードバランサ種別情報がない場合
        if (types == null) {
            return;
        }

        // ロードバランサ種別情報をテーブルに追加
        int n = 0;
        for (String type : types) {
            // ロードバランサ種別名
            // TODO: アイコン名取得ロジックのリファクタリング
            Icons typeIcon = Icons.NONE;
            String typeString = ViewProperties.getLoadBalancerType(type);

            Label nlbl = new Label("<img src=\"" + VaadinUtils.getIconPath(apl, typeIcon) + "\"><div>" + typeString
                    + "</div>", Label.CONTENT_XHTML);
            nlbl.setHeight(COLUMN_HEIGHT);

            n++;
            typeTable.addItem(new Object[] { n, nlbl }, type);
        }

        String type = null;
        if (types.size() > 0) {
            type = types.get(0);
        }

        // 先頭のロードバランサ種別を選択する
        typeTable.select(type);
    }

    private void showServices() {
        serviceTable.removeAllItems();

        // 割り当てサービス情報をテーブルに追加
        for (int i = 0; i < componentDtos.size(); i++) {
            ComponentDto componentDto = componentDtos.get(i);

            // サービス名
            //          String name = componentDto.getComponent().getComponentName();
            String name;
            if (StringUtils.isEmpty(componentDto.getComponent().getComment())) {
                name = componentDto.getComponent().getComponentName();
            } else {
                name = componentDto.getComponent().getComment() + "\n["
                        + componentDto.getComponent().getComponentName() + "]";
            }
            Label nameLabel = new Label(name, Label.CONTENT_PREFORMATTED);

            // サービス種類
            ComponentType componentType = componentDto.getComponentType();
            String typeName = componentType.getComponentTypeNameDisp();
            Icons typeIcon = Icons.fromName(componentType.getComponentTypeName());
            Label typeLabel = new Label("<img src=\"" + VaadinUtils.getIconPath(apl, typeIcon) + "\"><div>" + typeName
                    + "</div>", Label.CONTENT_XHTML);
            typeLabel.setHeight(COLUMN_HEIGHT);

            //            serviceTable.addItem(new Object[] { (i + 1), name, typeLabel }, componentDto.getComponent().getComponentNo());

            serviceTable.addItem(new Object[] { nameLabel, typeLabel }, componentDto.getComponent().getComponentNo());
        }

        Long componentNo = null;
        if (componentDtos.size() > 0) {
            componentNo = componentDtos.get(0).getComponent().getComponentNo();
        }

        // 先頭のサービス情報を選択する
        serviceTable.select(componentNo);
    }

    private void addButtonClick(ClickEvent event) {
        // 入力値を取得
        String loadBalancerName = (String) loadBalancerNameField.getValue();
        String comment = (String) commentField.getValue();
        Long platformNo = (Long) cloudTable.getValue();
        String type = (String) typeTable.getValue();
        Long componentNo = (Long) serviceTable.getValue();

        // TODO: 入力チェック
        try {
            loadBalancerNameField.validate();
            commentField.validate();
        } catch (InvalidValueException e) {
            DialogConfirm dialog = new DialogConfirm(ViewProperties.getCaption("dialog.error"), e.getMessage());
            getApplication().getMainWindow().addWindow(dialog);
            return;
        }
        if (platformNo == null) {
            DialogConfirm dialog = new DialogConfirm(ViewProperties.getCaption("dialog.error"), ViewMessages
                    .getMessage("IUI-000023"));
            getApplication().getMainWindow().addWindow(dialog);
            return;
        }
        if (type == null || type.length() == 0) {
            DialogConfirm dialog = new DialogConfirm(ViewProperties.getCaption("dialog.error"), ViewMessages
                    .getMessage("IUI-000054"));
            getApplication().getMainWindow().addWindow(dialog);
            return;
        }
        if (componentNo == null) {
            DialogConfirm dialog = new DialogConfirm(ViewProperties.getCaption("dialog.error"), ViewMessages
                    .getMessage("IUI-000065"));
            getApplication().getMainWindow().addWindow(dialog);
            return;
        }
        if (loadBalancerName.startsWith("lb-")) {
            DialogConfirm dialog = new DialogConfirm(ViewProperties.getCaption("dialog.error"), ViewMessages
                    .getMessage("IUI-000084", loadBalancerName));
            getApplication().getMainWindow().addWindow(dialog);
            return;
        }

        // クラウド番号
        Long farmNo = ViewContext.getFarmNo();
        Long loadBalancerNo = null;
        LoadBalancerService loadBalancerService = BeanContext.getBean(LoadBalancerService.class);

        // TODO: 固定文字列を外部化・定数化
        if ("aws".equals(type)) {
            // AWSロードバランサを作成
            try {
                loadBalancerNo = loadBalancerService.createAwsLoadBalancer(farmNo, loadBalancerName, comment, platformNo, componentNo);
            } catch (AutoApplicationException e) {
                String message = ViewMessages.getMessage(e.getCode(), e.getAdditions());
                DialogConfirm dialog = new DialogConfirm(ViewProperties.getCaption("dialog.error"), message);
                getApplication().getMainWindow().addWindow(dialog);
                return;
            }
        } else if ("ultramonkey".equals(type)) {
            // UltraMonkeyロードバランサを作成
            try {
                loadBalancerNo = loadBalancerService.createUltraMonkeyLoadBalancer(farmNo, loadBalancerName, comment, platformNo,
                        componentNo);
            } catch (AutoApplicationException e) {
                String message = ViewMessages.getMessage(e.getCode(), e.getAdditions());
                DialogConfirm dialog = new DialogConfirm(ViewProperties.getCaption("dialog.error"), message);
                getApplication().getMainWindow().addWindow(dialog);
                return;
            }
        } else if ("cloudstack".equals(type)) {
            // cloudstackロードバランサを作成
            try {
                loadBalancerNo = loadBalancerService.createCloudstackLoadBalancer(farmNo, loadBalancerName, comment, platformNo,
                        componentNo);
            } catch (AutoApplicationException e) {
                String message = ViewMessages.getMessage(e.getCode(), e.getAdditions());
                DialogConfirm dialog = new DialogConfirm(ViewProperties.getCaption("dialog.error"), message);
                getApplication().getMainWindow().addWindow(dialog);
                return;
            }
        }

        //TODO LOG
        AutoApplication aapl =  (AutoApplication)apl;
        aapl.doOpLog("LOAD_BALANCER", "Make Load_Balancer", null, null, loadBalancerNo, null);

        // 画面を閉じる
        close();
    }

}
