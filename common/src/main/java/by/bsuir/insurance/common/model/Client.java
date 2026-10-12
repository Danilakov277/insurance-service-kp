package by.bsuir.insurance.common.model;

import java.time.LocalDate;
import java.time.Period;

/** Клиент страховой компании (страхователь). */
public class Client extends Person {
    private static final long serialVersionUID = 1L;

    private LocalDate birthDate;
    private String passportNo;
    private String phone;
    private String address;

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getPassportNo() { return passportNo; }
    public void setPassportNo(String passportNo) { this.passportNo = passportNo; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    /** Полных лет на указанную дату. */
    public int getAge(LocalDate on) {
        return Period.between(birthDate, on).getYears();
    }

    @Override
    public Role getRole() {
        return Role.CLIENT;
    }
}
