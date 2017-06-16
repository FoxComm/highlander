import React from 'react';

export default function ({ children }) {
  return (
    <div>
      <img
        className="logo"
        src="https://s3-us-west-1.amazonaws.com/foxcomm-images/Fox_Head_Words.svg"
      />
      <div className="version" dangerouslySetInnerHTML={{ __html: children }} />
    </div>
  );
}

