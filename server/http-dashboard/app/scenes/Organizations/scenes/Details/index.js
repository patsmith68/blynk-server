import React                from 'react';
import {MainLayout}         from 'components';
import {connect}            from 'react-redux';
import {Roles}              from 'services/Roles';
import {Manage}             from 'services/Organizations';
import {ProductsFetch}      from 'data/Product/api';
import {
  Button,
  Tabs,
  message,
  Popconfirm
}                           from 'antd';
import PropTypes            from 'prop-types';
import {
  Map,
  List,
  fromJS
}                           from 'immutable';
import {bindActionCreators} from 'redux';
import {reset, SubmissionError}              from 'redux-form';
import {
  OrganizationsManageUsersFetch,
  OrganizationsDetailsUpdate,
  OrganizationsFetch,
  OrganizationsUsersFetch,
  OrganizationsDelete,
}                           from 'data/Organizations/actions';

import {
  OrganizationUsersDelete,
  OrganizationSendInvite
}                           from 'data/Organization/actions';

import {
  Info,
  Products,
  Admins
}                           from './components';

const {TabPane} = Tabs;

import './styles.less';

@connect((state) => ({
  list: state.Organizations.get('list') || null,
  details: state.Organizations.get('details'),
}), (dispatch) => ({
  resetForm: bindActionCreators(reset, dispatch),
  fetchProducts: bindActionCreators(ProductsFetch, dispatch),
  OrganizationsFetch: bindActionCreators(OrganizationsFetch, dispatch),
  OrganizationsDelete: bindActionCreators(OrganizationsDelete, dispatch),
  OrganizationSendInvite: bindActionCreators(OrganizationSendInvite, dispatch),
  OrganizationsUsersFetch: bindActionCreators(OrganizationsUsersFetch, dispatch),
  OrganizationUsersDelete: bindActionCreators(OrganizationUsersDelete, dispatch),
  OrganizationsDetailsUpdate: bindActionCreators(OrganizationsDetailsUpdate, dispatch),
  OrganizationsManageUsersFetch: bindActionCreators(OrganizationsManageUsersFetch, dispatch),
}))
class Details extends React.Component {

  static contextTypes = {
    router: PropTypes.object
  };

  static propTypes = {
    list: PropTypes.instanceOf(List),
    details: PropTypes.instanceOf(Map),

    params: PropTypes.object,

    resetForm: PropTypes.func,
    fetchProducts: PropTypes.func,
    OrganizationsFetch: PropTypes.func,
    OrganizationsDelete: PropTypes.func,
    OrganizationSendInvite: PropTypes.func,
    OrganizationsUsersFetch: PropTypes.func,
    OrganizationUsersDelete: PropTypes.func,
    OrganizationsDetailsUpdate: PropTypes.func,
    OrganizationsManageUsersFetch: PropTypes.func,
  };

  constructor(props) {
    super(props);

    this.handleAddAdmin = this.handleAddAdmin.bind(this);
    this.handleTabChange = this.handleTabChange.bind(this);
    this.handleUsersDelete = this.handleUsersDelete.bind(this);
    this.handleOrganizationEdit = this.handleOrganizationEdit.bind(this);
    this.handleUserInviteSuccess = this.handleUserInviteSuccess.bind(this);
    this.handleOrganizationDelete = this.handleOrganizationDelete.bind(this);
  }

  componentWillMount() {

    const redirectIfNotExist = (list) => {
      if (!list.find(org => org.get('id') === Number(this.props.params.id)))
        this.context.router.push('/organizations/?notFound=true');
    };

    if (!this.props.list) {
      this.props.OrganizationsFetch().then((response) => {
        redirectIfNotExist(fromJS(response.payload.data));
      });
    }

    if (!this.props.details.get('users')) {
      this.props.OrganizationsUsersFetch({
        id: this.props.params.id
      });
    }

    if (this.props.list)
      redirectIfNotExist(this.props.list);

  }

  componentWillUnmount() {
    this.props.OrganizationsDetailsUpdate(
      this.props.details
        .set('activeTab', this.TABS.INFO)
        .set('users', null)
    );
  }

  handleOrganizationEdit() {

    this.props.OrganizationsDetailsUpdate(this.props.details.set('loading', true));

    return Promise.all([
      this.props.OrganizationsFetch(),
      this.props.fetchProducts(),
      this.props.OrganizationsManageUsersFetch({id: this.props.params.id})
    ]).then(() => {
      setTimeout(() => {
        this.props.OrganizationsDetailsUpdate(this.props.details.set('loading', false));
        this.context.router.push(`/organizations/edit/${this.props.params.id}`);
      }, 500);
    });
  }


  TABS = {
    INFO: 'info',
    PRODUCTS: 'products',
    ADMINS: 'admins'
  };

  toggleOrganizationDeleteLoading(state) {
    this.props.OrganizationsDetailsUpdate(
      this.props.details.set('organizationDeleteLoading', state)
    );
  }

  handleOrganizationDelete() {
    this.toggleOrganizationDeleteLoading(true);

    this.props.OrganizationsDelete({
      id: this.props.params.id
    }).then(() => {

      this.props.OrganizationsFetch().then(() => {

        this.context.router.push('/organizations');

        this.toggleOrganizationDeleteLoading(false);
      });

    }).catch((response) => {
      this.toggleOrganizationDeleteLoading(false);

      const data = response.error.response.data;

      message.error(data.error && data.error.message || 'Cannot delete organization');
    });
  }

  handleUsersDelete(ids) {
    this.props.OrganizationsDetailsUpdate(this.props.details.set('userDeleteLoading', true));
    return new Promise((resolve) => {
      this.props.OrganizationUsersDelete(this.props.params.id, ids).then(() => {
        this.props.OrganizationsUsersFetch({
          id: this.props.params.id
        }).then(() => {
          this.props.OrganizationsDetailsUpdate(this.props.details.set('userDeleteLoading', false));
          resolve(true);
        });
      });
    });
  }

  handleAddAdmin(user) {
    this.props.OrganizationsDetailsUpdate(this.props.details.set('userInviteLoading', true));

    return (new Promise((resolve, reject) => {
      this.props.OrganizationSendInvite({
        id: this.props.params.id,
        email: user.email,
        name: user.name,
        role: Roles.ADMIN.value
      }).then(() => {
        this.props.OrganizationsUsersFetch({
          id: this.props.params.id
        }).then(() => {
          this.props.OrganizationsDetailsUpdate(this.props.details.set('userInviteLoading', false));

          resolve();
        });
      }).catch((response) => {
        const data = response.error.response.data;

        this.props.OrganizationsDetailsUpdate(this.props.details.set('userInviteLoading', false));

        reject(data);
      });
    })).catch((data) => {

      if (data && data.error && data.error.message) {
        throw new SubmissionError({'email': data.error.message});
      } else {
        message.error(data && data.error && data.error.message || 'Cannot invite user');
        throw new SubmissionError();
      }

    });
  }

  handleTabChange(tab) {
    this.props.OrganizationsDetailsUpdate(
      this.props.details.set('activeTab', tab)
    );
  }

  handleUserInviteSuccess() {
    message.success('Invite has been sent');
    this.props.resetForm(Manage.ADMIN_INVITE_FORM_NAME);
  }

  render() {

    if (!this.props.list)
      return null;

    if (!this.props.details.get('users'))
      return null;

    const organization = this.props.list.find(org => org.get('id') === Number(this.props.params.id));

    if (!organization)
      return null;

    return (
      <MainLayout>
        <MainLayout.Header title={organization.get('name')}
                           options={(
                             <div>
                               <Popconfirm title="Are you sure？" okText="Yes" cancelText="No"
                                           onConfirm={this.handleOrganizationDelete}>
                                 <Button type="danger">Delete</Button>
                               </Popconfirm>
                               <Button type="primary" loading={this.props.details.get('loading')}
                                       onClick={this.handleOrganizationEdit}>Edit</Button>
                             </div>
                           )}/>
        <MainLayout.Content className="product-details-content">
          <Tabs onChange={this.handleTabChange}
                activeKey={this.props.details.get('activeTab')}>
            <TabPane tab="Info"
                     key={this.TABS.INFO}>
              <div className="organizations-manage-tab-wrapper">
                <Info name={organization.get('name')}
                      description={organization.get('description')}
                      logoUrl={organization.get('logoUrl')}
                      canCreateOrgs={organization.get('canCreateOrgs')}/>
              </div>
            </TabPane>
            <TabPane tab="Products"
                     key={this.TABS.PRODUCTS}>
              <div className="organizations-manage-tab-wrapper">
                <Products products={organization.get('products')}/>
              </div>
            </TabPane>
            <TabPane tab="Admins"
                     key={this.TABS.ADMINS}>
              <div className="organizations-manage-tab-wrapper">
                <Admins onUsersDelete={this.handleUsersDelete}
                        onUserAdd={this.handleAddAdmin}
                        onUserInviteSuccess={this.handleUserInviteSuccess}
                        userDeleteLoading={this.props.details.get('userDeleteLoading')}
                        userInviteLoading={this.props.details.get('userInviteLoading')}
                        users={this.props.details.get('users').filter(user => user.get('role') === Roles.ADMIN.value)}/>
              </div>
            </TabPane>
          </Tabs>
        </MainLayout.Content>
      </MainLayout>
    );
  }

}

export default Details;
