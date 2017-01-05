// @flow
import React from 'react';
import styles from './panel.css';

// components
import Panel from '../panel/panel';
import Participants from './participants';

// types
import type { EntityType } from 'types/entity';

type Props = {
  entity: EntityType,
}

const ParticipantsPanel = (props: Props) => {
  return (
    <Panel styleName="root">
      <div styleName="container">
        <Participants
          entity={props.entity}
          group="watchers"
          title="Watchers"
          emptyTitle="Unwatched"
          actionTitle="watch"
        />
      </div>
    </Panel>
  );
};

export default ParticipantsPanel;
