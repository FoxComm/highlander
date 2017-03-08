/* @flow */

import React, { Element } from 'react';

import { anyPermitted, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';
import { IndexLink, Link } from 'components/link';

import type { Claims } from 'lib/claims';

import styles from './entries.css';

const userClaims = readAction(frn.settings.user);
const pluginClaims = readAction(frn.settings.plugin);
const applicationClaims = readAction(frn.settings.application);

const SettingsEntry = ({ claims, routes }: TMenuEntry) => {
    const allClaims = { ...userClaims, ...pluginClaims, ...applicationClaims };

    if (!anyPermitted(allClaims, claims)) {
      return (
        <div styleName="fc-entries-wrapper">
          <h3>SETTINGS</h3>
          <li>
            <NavigationItem
              to='integrations'
              icon='integrations'
              title='Integrations'
              routes={routes}
              actualClaims='none'
              expectedClaims='none'
            />
          </li>
          </div>
      );
    }

    return (
      <div styleName="fc-entries-wrapper">
        <h3>SETTINGS</h3>
        <li>
          <NavigationItem
            to="users"
            icon="customers"
            title="Users"
            routes={routes}
            actualClaims={claims}
            expectedClaims={userClaims}
          />
        </li>
        <li>
          <NavigationItem
            to="plugins"
            icon="plugins"
            title="Plugins"
            routes={routes}
            actualClaims={claims}
            expectedClaims={pluginClaims}
          />
        </li>
        <li>
          <NavigationItem
            to="applications"
            icon="applications"
            title="Applications"
            routes={routes}
            actualClaims={claims}
            expectedClaims={applicationClaims}
          />
        </li>
      </div>
    );
};
export default SettingsEntry;
