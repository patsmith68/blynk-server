import React from 'react';
import {
  AddWidgetTools,
  // DeviceSelect,
} from './components';
import PropTypes from 'prop-types';
import {getNextId} from 'services/Products';
// import {ProductDashboardDeviceIdForPreviewChange} from 'data/Product/actions';
import {fromJS, List, Map} from 'immutable';
import {getCoordinatesToSet, buildDataQueryRequestForWidgets} from 'services/Widgets';
import './styles.less';
import _ from 'lodash';
import {connect} from 'react-redux';
// import {bindActionCreators} from 'redux';
// import {DevicesListForProductDashboardPreviewFetch} from 'data/Product/api';
// import {WidgetsHistory} from 'data/Widgets/api';
import {Fields, Field} from 'redux-form';
import {Grids} from 'components';
import {WidgetEditable} from "components/Widgets";

@connect((state) => ({
  orgId: Number(state.Account.orgId),
  // devicesListForPreview: fromJS(state.Product.dashboardPreview.devicesList || []),
  // devicePreviewId: Number(state.Product.dashboardPreview.selectedDeviceId),
  // devicesLoading: state.Devices.devicesLoading,
  // history: state.Devices && state.Devices.getIn(['Widgets', 'widgetsData']),
}), (/*dispatch*/) => ({
  // changeDeviceIdForPreview: bindActionCreators(ProductDashboardDeviceIdForPreviewChange, dispatch),
  // fetchDevicesListForPreview: bindActionCreators(DevicesListForProductDashboardPreviewFetch, dispatch),
  // fetchWidgetHistory: bindActionCreators(WidgetsHistory, dispatch),
}))
class Dashboard extends React.Component {

  static propTypes = {
    onWidgetAdd             : PropTypes.func,
    onWidgetsChange         : PropTypes.func,
    fetchWidgetHistory      : PropTypes.func,
    changeDeviceIdForPreview: PropTypes.func,

    orgId    : PropTypes.number,
    productId: PropTypes.number,

    devicePreviewId: PropTypes.number,

    devicesListForPreview: PropTypes.instanceOf(List),
    widgets              : PropTypes.instanceOf(List),
    history              : PropTypes.instanceOf(Map),


    fetchDevicesListForPreview: PropTypes.func,

    isDevicePreviewEnabled: PropTypes.bool,
    devicesLoading: PropTypes.bool,

    fields: PropTypes.object,
  };

  constructor(props) {
    super(props);

    this.handleWidgetAdd = this.handleWidgetAdd.bind(this);
    this.handleWidgetDelete = this.handleWidgetDelete.bind(this);
    this.handleWidgetClone = this.handleWidgetClone.bind(this);
    // this.handleDevicePreviewIdChange = this.handleDevicePreviewIdChange.bind(this);
  }

  componentWillMount() {
    // if(this.props.isDevicePreviewEnabled)
    //   this.props.fetchDevicesListForPreview({
    //     orgId    : this.props.orgId,
    //     productId: this.props.productId,
    //   });
  }

  componentDidUpdate(prevProps) {

    if(this.props.isDevicePreviewEnabled && prevProps.devicePreviewId !== this.props.devicePreviewId) {
      this.getDataForWidgets();
    }

    if(this.isAnyDataStreamUpdated(prevProps.fields.getAll(), this.props.fields.getAll())) {
      this.getDataForWidgets();
    }
  }

  _simplifyToSource(arr = []){
    return _.sortBy(fromJS(arr).map((item) => ({
      id : item.get('id'),
      pin: item.getIn(['sources', '0', 'dataStream', 'pin']) || null,
    })).toJS(), ['id']);
  }

  isAnyDataStreamUpdated(oldFields, newFields) {
    oldFields = this._simplifyToSource(oldFields);
    newFields = this._simplifyToSource(newFields);

    let length = Math.max(oldFields.length, newFields.length);

    for (let i = 0; i < length; i++) {
      if (!oldFields[i]) continue;
      if (!newFields[i]) continue;

      if (oldFields[i].id === newFields[i].id && Number(oldFields[i].pin) !== Number(newFields[i].pin)) {
        return true;
      }
    }
  }

  getDataForWidgets() {


    let dataQueryRequests = [];

    if (this.props.fields.getAll() && this.props.fields.getAll().length && this.props.devicePreviewId)

      dataQueryRequests = buildDataQueryRequestForWidgets({
        widgets: this.props.fields.getAll(),
        deviceId: this.props.devicePreviewId,
        timeFrom: new Date().getTime() - 1000 * 60 * 60 * 24 * 7, // 7 days ago,
        timeTo: new Date().getTime()
      });

    if (dataQueryRequests.length)
      this.props.fetchWidgetHistory({
        deviceId: this.props.devicePreviewId,
        dataQueryRequests: dataQueryRequests,
      });

  }

  handleWidgetDelete(id) {
    let fieldIndex = null;

    this.props.fields.getAll().forEach((field, index) => {
      if(Number(field.id) === Number(id))
        fieldIndex = index;
    });

    this.props.fields.remove(fieldIndex);
  }

  handleWidgetClone(id) {

    const widgets = this.props.fields.getAll();

    const widget = _.find(widgets, (widget) => Number(widget.id) === id);

    const coordinatesForNewWidget = getCoordinatesToSet(widget, widgets, 'lg');

    this.props.fields.push({
      ...widget,
      id: getNextId(this.props.fields.getAll()),
      label: `${widget.label} Copy`,
      x: coordinatesForNewWidget.x,
      y: coordinatesForNewWidget.y,
    });

  }

  handleWidgetAdd(widget) {

    const widgets = this.props.fields.getAll();

    const coordinatesForNewWidget = getCoordinatesToSet(widget, widgets, 'lg'); //hardcoded breakPoint as we have only lg for now

    this.props.fields.push({
      ...widget,
      id: getNextId(this.props.fields.getAll()),
      x: coordinatesForNewWidget.x,
      y: coordinatesForNewWidget.y,
      width: widget.w,
      height: widget.h,
    });
  }

  // handleDevicePreviewIdChange(id) {
  //   this.props.changeDeviceIdForPreview(id);
  // }

  render() {

    // const {devicesListForPreview} = this.props;

    const widgets = fromJS(this.props.fields.map((prefix, index, fields) => {
      const field = fields.get(index);
      return {
        ...field,
        fieldName: prefix,
        w: field.width,
        h: field.height,
      };
    }));

    const names = widgets.map((widget) => (widget.get('fieldName'))).toJS();

    const gridWidgets = widgets.map((widget) => {

      // const history = this.props.history.getIn([
      //   String(this.props.devicePreviewId),
      //   String(widget.get('id'))
      // ]);
      //
      // const loading = this.props.history.getIn([
      //   String(this.props.devicePreviewId),
      //   'loading'
      // ]);

      return (
        <Field name={`${widget.get('fieldName')}`}
               key={widget.get('id')}
               component={WidgetEditable}
               // deviceId={Number(this.props.devicePreviewId)}
               onWidgetDelete={this.handleWidgetDelete}
               onWidgetClone={this.handleWidgetClone}
               // loading={loading}
               // history={history}
        />
      );
    }).toJS();

    return (
      <div className="products-manage-dashboard">

        <div className={`products-manage-dashboard--tools`}>
          <div className={`products-manage-dashboard--tools--widget-add`}>
            <AddWidgetTools onWidgetAdd={this.handleWidgetAdd}/>
          </div>
          { /* this.props.isDevicePreviewEnabled && (
            <div className={`products-manage-dashboard--tools--device-select`}>
              <DeviceSelect loading={this.props.devicesLoading} devicesList={devicesListForPreview}
                            value={Number(this.props.devicePreviewId)} onChange={this.handleDevicePreviewIdChange}/>
            </div>
          ) */}
        </div>

        <Fields names={names} widgets={gridWidgets} component={Grids.GridManage} /*deviceId={this.props.devicePreviewId}*/ />

      </div>
    );
  }

}

export default Dashboard;
