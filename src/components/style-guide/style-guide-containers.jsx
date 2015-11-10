import React from 'react';
import ContentBox from '../content-box/content-box';
import Panel from '../panel/panel';

const StyleGuideContainers = props => {
  return (
    <div>
      <div className="fc-grid fc-grid-md-1-4">
        <div>
          <ContentBox title="fc-content-box With Title">
            <p>
              Here is some content for the box.
            </p>
          </ContentBox>
        </div>
      </div>
      <div className="fc-grid fc-grid-md-1-4">
        <div>
          <Panel title="fc-panel with title">
            <p>
              Here is some content for the panel.
            </p>
          </Panel>
        </div>
      </div>
    </div>
  );
};

export default StyleGuideContainers;
