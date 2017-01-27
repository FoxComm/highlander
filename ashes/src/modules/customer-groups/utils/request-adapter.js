/* @flow */

const requestAdapter = (groupId: number) => {
  const matchRule = { term: { 'groups': groupId } };

  return matchRule;
};

export default requestAdapter;
