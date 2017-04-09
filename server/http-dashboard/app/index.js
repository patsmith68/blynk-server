import React from 'react';
import ReactDOM from 'react-dom';
import {Router, Route, hashHistory, Redirect} from 'react-router';

/* components */
import Layout from './components/Layout';
import UserLayout from './components/UserLayout';
import LoginLayout from './components/LoginLayout';

/* scenes */
import Login from './scenes/Login';
import ForgotPass from './scenes/ForgotPass';
import ResetPass from './scenes/ResetPass';
import Logout from './scenes/Logout';
import {MyAccount, OrganizationSettings} from './scenes/UserProfile';

/* store */
import {Provider} from 'react-redux';
import Store from './store';

/* services */
import {RouteGuestOnly, RouteAuthorizedOnly} from './services/Login';

/* vendor */
import {LocaleProvider} from 'antd';
import enUS from 'antd/lib/locale-provider/en_US';

Store().then((store) => {

  ReactDOM.render(
    <Provider store={store}>
      <LocaleProvider locale={enUS}>
        <Router history={hashHistory}>
          <Route component={Layout}>
            <Route component={UserLayout} onEnter={RouteAuthorizedOnly(store)}>
              <Route path="/account" component={MyAccount}/>
              <Route path="/organization-settings" component={OrganizationSettings}/>
            </Route>
            <Route path="/logout" component={Logout}/>
            <Route component={LoginLayout}>
              <Route path="/login" component={Login} onEnter={RouteGuestOnly(store)}/>
              <Route path="/forgot-pass" component={ForgotPass} onEnter={RouteGuestOnly(store)}/>
              <Route path="/resetpass" component={ResetPass}/>
            </Route>
          </Route>
          <Redirect from="*" to="/login"/>
        </Router>
      </LocaleProvider>
    </Provider>,
    document.getElementById('app')
  );

});
