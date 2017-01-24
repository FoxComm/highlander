/* @flow */

import React from 'react';
import { Link } from 'components/link';

import ContentBox from 'components/content-box/content-box';

import styles from './groups.css';

type Props = {
  groups: Array<TCustomerGroupShort>;
};

export default (props: Props) => (
  <ContentBox title="Groups">
    {props.groups.map((group: TCustomerGroupShort) => (
      <Link className={styles.group} to="customer-group" params={{groupId: group.id}} key={group.id}>
        <span className={styles.name}><i className="icon icon-customers"></i> {group.name}</span>
        <span className={styles.type}>{group.type}</span>
      </Link>
    ))}
  </ContentBox>
);
