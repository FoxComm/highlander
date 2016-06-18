package gormfox

import (
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"

	"testing"

	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/common/validation"
)

func TestRepository(t *testing.T) {
	RegisterFailHandler(Fail)
	RunSpecs(t, "Repository and Model Test")
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

var _ = Describe("Lifecycle of a model with has one, has many and many to many associations", func() {

	Context("#NewRepository", func() {
		It("successfully instantiates a new repository", func() {
			_, err := NewRepository()
			Expect(err).ToNot(HaveOccurred())

		})

		Context("Behaviours for models", func() {
			var (
				repository Repository
				car        Car
			)

			BeforeEach(func() {
				repository, _ = NewRepository()

				tasks.TruncateTables([]string{
					"cars",
					"license_plate_numbers",
					"parts",
					"destinations",
				})

				db, _ := config.DefaultConnection()
				//Migrate them
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

				car = Car{
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
			})

			It("Creation", func() {
				err := repository.Create(&car)
				Expect(err).ToNot(HaveOccurred())
			})

			Context("Finding", func() {
				BeforeEach(func() {
					repository.Create(&car)
				})

				It("the created parent object", func() {
					c := &Car{}
					carModel, err := repository.FindByID(c, car.ID)
					Expect(err).ToNot(HaveOccurred())
					Expect(carModel.Identifier()).To(Equal(car.ID))
				})

				It("via a FindAll", func() {
					//Create a second car, so we have > 1
					car2 := &Car{}
					err := repository.Create(car2)
					Expect(err).ToNot(HaveOccurred())

					var cars []Car
					foundCars, err := repository.FindAll(&cars)
					Expect(err).ToNot(HaveOccurred())
					c := foundCars.(*[]Car)

					foundCar := (*c)[1]
					Expect(foundCar.ID).To(Equal(car2.ID))
				})

				It("the has one association", func() {
					c := &Car{}
					carModel, err := repository.FindByID(c, car.ID, HasOne("LicensePlateNumber"))
					Expect(err).ToNot(HaveOccurred())
					foundCar := carModel.(*Car)
					Expect(foundCar.LicensePlateNumber.Name).To(Equal("SCHLOMO88"))
				})

				It("the has many association", func() {
					c := &Car{}
					carModel, err := repository.FindByID(c, car.ID, HasMany("Parts"))
					Expect(err).ToNot(HaveOccurred())
					foundCar := carModel.(*Car)
					Expect(len(foundCar.Parts)).To(Equal(2))
				})

				It("the many to many association", func() {
					var dests []Destination
					c := &Car{}

					ass := ManyToMany{Name: "Destinations", Collection: &dests}
					_, err := repository.FindByID(c, car.ID, ass)
					Expect(err).ToNot(HaveOccurred())

					Expect(len(dests)).To(Equal(2))
				})
			})

			Context("Updating", func() {
				BeforeEach(func() {
					err := repository.Create(&car)
					if err != nil {
						panic(err)
					}
				})

				It("works", func() {
					c := &Car{}
					carModel, err := repository.FindByID(c, car.ID, HasOne("LicensePlateNumber"))
					foundCar := carModel.(*Car)
					foundCar.LicensePlateNumber.Name = "88MOFOYOLO"
					err = repository.Update(&foundCar.LicensePlateNumber)
					Expect(err).ToNot(HaveOccurred())

					sw := &LicensePlateNumber{}
					wheel, err := repository.FindByID(sw, foundCar.LicensePlateNumber.ID)
					Expect(err).ToNot(HaveOccurred())
					sw2 := wheel.(*LicensePlateNumber)
					Expect(sw2.Name).To(Equal("88MOFOYOLO"))
				})
			})

			Context("Deleting", func() {
				BeforeEach(func() {
					repository.Create(&car)
				})
				It("works", func() {
					carID := car.ID
					err := repository.Delete(car)
					Expect(err).ToNot(HaveOccurred())
					c := &Car{}
					_, err = repository.FindByID(c, carID)
					Expect(err).To(HaveOccurred())
					Expect(err.Error()).To(Equal("record not found"))
				})
			})
		})
	})
})

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
