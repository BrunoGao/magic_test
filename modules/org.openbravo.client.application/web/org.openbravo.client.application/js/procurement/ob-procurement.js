/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.PROC = OB.PROC || {};

/**
 * Modify base Quantity when the Aum Quantity changes
 */

OB.PROC.CreateLinesOnChangeQuantityAum = function (item, view, form, grid) {
  var aumQty = item.getValue();
  var record = item.grid.getSelectionObject().lastSelectionItem;
  var aum = item.form.getItems()[item.grid.getColNum('aum')].getValue();
  if (aumQty !== undefined) {
    OB.RemoteCallManager.call('org.openbravo.common.actionhandler.GetConvertedQtyActionHandler', {
      mProductId: record.product,
      qty: aumQty,
      toUOM: aum,
      reverse: false
    }, {}, function (response, data, request) {
      if (data.qty) {
        item.grid.setEditValue(item.grid.getEditRow(), 'orderedQuantity', data.qty);
      }
    });
  }
};

/**
 * Modify base Aum Quantity when the base Quantity changes
 */

OB.PROC.CreateLinesOnChangeQuantity = function (item, view, form, grid) {
  var qty = item.getValue();
  var record = grid.getSelectionObject().lastSelectionItem;
  var aum = item.grid.getEditValues(item.grid.getRecordIndex(item.record)).aum;
  if (qty !== undefined) {
    OB.RemoteCallManager.call('org.openbravo.common.actionhandler.GetConvertedQtyActionHandler', {
      mProductId: record.product,
      qty: qty,
      toUOM: aum,
      reverse: true
    }, {}, function (response, data, request) {
      if (data.qty) {
        item.grid.setEditValue(item.grid.getEditRow(), 'aumQuantity', data.qty);
      }
    });
  }
};

/**
 * Modify base Quantity when the Aum selected changes
 */

OB.PROC.CreateLinesOnChangeAum = function (item, validator, value, record) {
  var aum = item.pickList.getSelection()[0].id;
  var changed_record = item.grid.getSelectionObject().lastSelectionItem;
  var aumQty = item.grid.getEditValues(item.grid.getRecordIndex(item.record)).aumQuantity;
  if (aumQty !== undefined) {
    OB.RemoteCallManager.call('org.openbravo.common.actionhandler.GetConvertedQtyActionHandler', {
      mProductId: changed_record.product,
      qty: aumQty,
      toUOM: aum,
      reverse: false
    }, {}, function (response, data, request) {
      if (data.qty) {
        item.grid.setEditValue(item.grid.getEditRow(), 'orderedQuantity', data.qty);
      }
    });
  }
};