package gormfox

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/common/validation"
)

type RepositoryTestSuite struct {
	suite.Suite
	repository Repository
	car        Car
	err        error
}

func TestRepositorySuite(t *testing.T) {
	suite.Run(t, new(RepositoryTestSuite))
}

func (suite *RepositoryTestSuite) SetupTest() {
	suite.repository, suite.err = NewRepository()

	if suite.err == nil {
		tasks.TruncateTables([]string{
			"cars",
			"license_plate_numbers",
			"parts",
			"destinations",
		})

		db, err := config.DefaultConnection()
		if assert.Nil(suite.T(), err) {
			db.AutoMigrate(&Car{})
			db.AutoMigrate(&LicensePlateNumber{})
			db.AutoMigrate(&Part{})
			db.AutoMigrate(&Destination{})

			seattle := Destination{
				Name: "Seattle",
			}

			sanFran := Destination{
				Name: "San Francisco",
			}

			suite.car = Car{
				LicensePlateNumber: LicensePlateNumber{
					Name: "SCHLOMO88",
				},
				Parts: []Part{
					Part{}, Part{},
				},
				Destinations: []Destination{
					seattle,
					sanFran,
				},
			}
		}
	}
}

func (suite *RepositoryTestSuite) TestNewRepository() {
	assert.Nil(suite.T(), suite.err)
}

func (suite *RepositoryTestSuite) TestCreation() {
	err := suite.repository.Create(&suite.car)
	assert.Nil(suite.T(), err)
}

func (suite *RepositoryTestSuite) TestFindParentObject() {
	err := suite.repository.Create(&suite.car)
	if assert.Nil(suite.T(), err) {
		c := &Car{}
		carModel, err := suite.repository.FindByID(c, suite.car.ID)
		if assert.Nil(suite.T(), err) {
			assert.Equal(suite.T(), suite.car.ID, carModel.Identifier())
		}
	}
}

func (suite *RepositoryTestSuite) TestFindParentObjectViaFindAll() {
	if err := suite.repository.Create(&suite.car); err != nil {
		assert.Nil(suite.T(), err)
		return
	}

	// Create a second car, so we have > 1
	car2 := &Car{}
	if err := suite.repository.Create(car2); err != nil {
		assert.Nil(suite.T(), err)
		return
	}

	var cars []Car
	foundCars, err := suite.repository.FindAll(&cars)
	if assert.Nil(suite.T(), err) {
		c := foundCars.(*[]Car)
		foundCar := (*c)[1]
		assert.Equal(suite.T(), car2.ID, foundCar.ID)
	}
}

func (suite *RepositoryTestSuite) TestHasOneAssociation() {
	if err := suite.repository.Create(&suite.car); err != nil {
		assert.Nil(suite.T(), err)
		return
	}

	c := &Car{}
	carModel, err := suite.repository.FindByID(c, suite.car.ID, HasOne("LicensePlateNumber"))
	if assert.Nil(suite.T(), err) {
		foundCar := carModel.(*Car)
		assert.Equal(suite.T(), "SCHLOMO88", foundCar.LicensePlateNumber.Name)
	}
}

func (suite *RepositoryTestSuite) TestHasManyAssociation() {
	if err := suite.repository.Create(&suite.car); err != nil {
		assert.Nil(suite.T(), err)
		return
	}

	c := &Car{}
	carModel, err := suite.repository.FindByID(c, suite.car.ID, HasMany("Parts"))
	if assert.Nil(suite.T(), err) {
		foundCar := carModel.(*Car)
		assert.Len(suite.T(), foundCar.Parts, 2, "The number of parts found was not 2")
	}
}

func (suite *RepositoryTestSuite) TestManyToManyAssociation() {
	if err := suite.repository.Create(&suite.car); err != nil {
		assert.Nil(suite.T(), err)
		return
	}

	var dests []Destination
	c := &Car{}

	ass := ManyToMany{Name: "Destinations", Collection: &dests}
	_, err := suite.repository.FindByID(c, suite.car.ID, ass)
	if assert.Nil(suite.T(), err) {
		assert.Len(suite.T(), dests, 2, "The number of dests found was not 2")
	}
}

func (suite *RepositoryTestSuite) TestUpdating() {
	car := suite.car
	if err := suite.repository.Create(&car); err != nil {
		assert.Nil(suite.T(), err)
		return
	}

	c := &Car{}
	carModel, err := suite.repository.FindByID(c, car.ID, HasOne("LicensePlateNumber"))
	foundCar := carModel.(*Car)
	foundCar.LicensePlateNumber.Name = "88MOFOYOLO"
	err = suite.repository.Update(&foundCar.LicensePlateNumber)
	if assert.Nil(suite.T(), err) {
		sw := &LicensePlateNumber{}
		wheel, err := suite.repository.FindByID(sw, foundCar.LicensePlateNumber.ID)
		if assert.Nil(suite.T(), err) {
			sw2 := wheel.(*LicensePlateNumber)
			assert.Equal(suite.T(), "88MOFOYOLO", sw2.Name)
		}
	}
}

type Car struct {
	ID                   uint
	LicensePlateNumberID uint
	LicensePlateNumber   LicensePlateNumber //has one
	Parts                []Part
	Destinations         []Destination `gorm:"many2many:car_destinations;"`
}

type LicensePlateNumber struct {
	ID   uint
	Name string
}

//Car has many Parts
type Part struct {
	ID    uint
	CarID uint
}

//Car has many to many Destinations
type Destination struct {
	ID   uint
	Name string
	Cars []Car `gorm:"many2many:car_destinations;"`
}

// func TestNewRepository(t *testing.T) {
// 	_, err := NewRepository()
// 	assert.NotNil(err)
// }

// var _ = Describe("Lifecycle of a model with has one, has many and many to many associations", func() {

// 			Context("Updating", func() {
// 				BeforeEach(func() {
// 					err := repository.Create(&car)
// 					if err != nil {
// 						panic(err)
// 					}
// 				})

// 				It("works", func() {
// 					c := &Car{}
// 					carModel, err := suite.repository.FindByID(c, car.ID, HasOne("LicensePlateNumber"))
// 					foundCar := carModel.(*Car)
// 					foundCar.LicensePlateNumber.Name = "88MOFOYOLO"
// 					err = repository.Update(&foundCar.LicensePlateNumber)
// 					Expect(err).ToNot(HaveOccurred())

// 					sw := &LicensePlateNumber{}
// 					wheel, err := suite.repository.FindByID(sw, foundCar.LicensePlateNumber.ID)
// 					Expect(err).ToNot(HaveOccurred())
// 					sw2 := wheel.(*LicensePlateNumber)
// 					Expect(sw2.Name).To(Equal("88MOFOYOLO"))
// 				})
// 			})

// 			Context("Deleting", func() {
// 				BeforeEach(func() {
// 					repository.Create(&car)
// 				})
// 				It("works", func() {
// 					carID := car.ID
// 					err := repository.Delete(car)
// 					Expect(err).ToNot(HaveOccurred())
// 					c := &Car{}
// 					_, err = suite.repository.FindByID(c, carID)
// 					Expect(err).To(HaveOccurred())
// 					Expect(err.Error()).To(Equal("record not found"))
// 				})
// 			})
// 		})
// 	})
// })

func (c Car) Validate(repository Repository) ([]validation.Invalid, error) {
	return nil, nil
}

func (c Car) Identifier() uint {
	return c.ID
}

func (c LicensePlateNumber) Validate(repository Repository) ([]validation.Invalid, error) {
	return nil, nil
}

func (c LicensePlateNumber) Identifier() uint {
	return c.ID
}

func (c Destination) Validate(repository Repository) ([]validation.Invalid, error) {
	return nil, nil
}

func (c Destination) Identifier() uint {
	return c.ID
}
