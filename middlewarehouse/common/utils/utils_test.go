package utils

import (
	"github.com/stretchr/testify/suite"
	"regexp"
	"testing"
)

type ReplaceAllMatchingGroupSuite struct {
	suite.Suite
	r *regexp.Regexp
}

func TestReplaceAllMatchingGroupSuite(t *testing.T) {
	suite.Run(t, new(ReplaceAllMatchingGroupSuite))
}

func (s *ReplaceAllMatchingGroupSuite) SetupSuite() {
	s.r, _ = regexp.Compile("pass(?:word|wd)?\":\"(.+?)\"")
}

func (s *ReplaceAllMatchingGroupSuite) TestReplaceAllMatchingGroup_ReturnSameStringWithNoMatches() {
	str := "emptystring"

	res := ReplaceAllMatchingGroup(str, "***", s.r)

	s.Equal(str, res)
}

func (s *ReplaceAllMatchingGroupSuite) TestReplaceAllMatchingGroup_Single() {
	str := "{\"passwd\":\"api$pass7!\"}"

	res := ReplaceAllMatchingGroup(str, "***", s.r)

	s.Equal("{\"passwd\":\"***\"}", res)
}

func (s *ReplaceAllMatchingGroupSuite) TestReplaceAllMatchingGroup_Multiple() {
	str := "{\"passwd\":\"api$pass7!\",\"org\":\"tenant\",\"password\":\"api$pass7!\" ,\"pass\":\"api$pass7!\"}"

	res := ReplaceAllMatchingGroup(str, "***", s.r)

	s.Equal("{\"passwd\":\"***\",\"org\":\"tenant\",\"password\":\"***\" ,\"pass\":\"***\"}", res)
}

func (s *ReplaceAllMatchingGroupSuite) TestSanitizePassword_ReturnSameStringWithNoMatches() {
	str := "emptystring"

	res := SanitizePassword([]byte(str))

	s.Equal(str, res)
}

func (s *ReplaceAllMatchingGroupSuite) TestSanitizePassword_Single() {
	str := "{\"passwd\":\"api$pass7!\"}"

	res := SanitizePassword([]byte(str))

	s.Equal("{\"passwd\":\"***\"}", res)
}

func (s *ReplaceAllMatchingGroupSuite) TestSanitizePassword_Multiple() {
	str := "{\"passwd\":\"api$pass7!\",\"org\":\"tenant\",\"password\":\"api$pass7!\" ,\"pass\":\"api$pass7!\"}"

	res := SanitizePassword([]byte(str))

	s.Equal("{\"passwd\":\"***\",\"org\":\"tenant\",\"password\":\"***\" ,\"pass\":\"***\"}", res)
}
