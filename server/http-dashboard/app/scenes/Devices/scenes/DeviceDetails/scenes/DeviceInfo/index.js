import React from 'react';
import {DeviceInfo} from 'scenes/Devices/components/Device/components';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import PropTypes from 'prop-types';
import {DeviceMetadataUpdate, DeviceDetailsFetch} from 'data/Devices/api';

@connect((state) => ({
  device: state.Devices.deviceDetails,
  account: state.Account,
  orgId: state.Organization.id,
}), (dispatch) => ({
  updateDeviceMetadata: bindActionCreators(DeviceMetadataUpdate, dispatch),
  fetchDevice: bindActionCreators(DeviceDetailsFetch, dispatch),
}))
class DeviceInfoScene extends React.Component {

  static propTypes = {
    account: PropTypes.object,
    device: PropTypes.object,

    orgId: PropTypes.number,

    fetchDevice: PropTypes.func,
    updateDeviceMetadata: PropTypes.func,
  };

  constructor(props) {
    super(props);

    this.handleMetadataChange = this.handleMetadataChange.bind(this);
  }

  handleMetadataChange(values) {
    return this.props.updateDeviceMetadata({
      orgId: this.props.orgId,
      deviceId: this.props.device.id,
    },values).then(() => {
      this.props.fetchDevice({
        orgId: this.props.orgId,
      }, {
        id: this.props.device.id
      });
    });
  }

  render() {

    const {device,account} = this.props;

    return (
      <DeviceInfo device={device} account={account} onMetadataChange={this.handleMetadataChange}/>
    );
  }

}

export default DeviceInfoScene;
