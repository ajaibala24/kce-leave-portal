package com.kce.leave.config;

import com.kce.leave.model.Student;
import com.kce.leave.model.Teacher;
import com.kce.leave.repository.StudentRepository;
import com.kce.leave.repository.TeacherRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Loads the whole class (II-CSE-A) into MongoDB every time the app starts.
 * Students already in the database are updated (no duplicates); new ones are added.
 * To add / correct a student, edit the list below: { "NAME", "REGISTER NO" }.
 *
 * Student login  -> username = Name (full name or first name), password = Register No
 * Teacher login  -> class = II-CSE-A, password = Arun.M
 */
@Component
public class DataSeeder implements CommandLineRunner {
    public static final String CLASS_NAME = "II-CSE-A";

    private static final String[][] STUDENTS = {
        {"AATHISH B", "25CSA01"},
        {"ABDUL RAHMAN", "25CSA02"},
        {"ABINAYA S", "25CSA03"},
        {"ADHIKA S S", "25CSA04"},
        {"AHAMED A", "25CSA05"},
        {"AISWARYA K G", "25CSA06"},
        {"AISWARYA S", "25CSA07"},
        {"AJAI BALA K", "25CSA08"},
        {"AMRUTHA A", "25CSA09"},
        {"ANUSH G", "25CSA10"},
        {"ARUTHRA GOVINDARAJ", "25CSA11"},
        {"ASHINI B", "25CSA12"},
        {"ASRIN SHIFANA M", "25CSA13"},
        {"ASVAN N", "25CSA14"},
        {"ASWIN M", "25CSA15"},
        {"AYYAPPAN M", "25CSA16"},
        {"BALAJI SHANMUGANATHAN S B", "25CSA17"},
        {"BALAKRISHNAN M", "25CSA18"},
        {"BARATH KUMAR L", "25CSA19"},
        {"BHARANI SUBRAJA S", "25CSA20"},
        {"BHAVYA S", "25CSA21"},
        {"BOPDHANUSYA S", "25CSA22"},
        {"DEVA DHARSHINI T", "25CSA23"},
        {"DHARANI K", "25CSA24"},
        {"DHARSHAN P", "25CSA25"},
        {"DHARSHINI J", "25CSA26"},
        {"DHARUN M", "25CSA27"},
        {"DIVYADHARSHINI S", "25CSA28"},
        {"ELAVARASAN T", "25CSA29"},
        {"ESWARAN S", "25CSA30"},
        {"FAHMITHA SULTHANA A", "25CSA31"},
        {"FARIHA M", "25CSA32"},
        {"FATHIMARUFAITHAM", "25CSA33"},
        {"GEETHA S", "25CSA34"},
        {"GOPINATH M", "25CSA35"},
        {"GUNASEELA N", "25CSA36"},
        {"HAMEEDHA M", "25CSA37"},
        {"HARANI S", "25CSA38"},
        {"HARIHARAN M", "25CSA39"},
        {"HARIHARAN R", "25CSA40"},
        {"HARINARAYANAN T", "25CSA41"},
        {"HARSHINI R", "25CSA42"},
        {"HEMALATHA S", "25CSA43"},
        {"IRFAN R", "25CSA44"},
        {"IRSANA RISMIN A", "25CSA45"},
        {"JANANI SRI R", "25CSA46"},
        {"JANNATHUL SUNOFIYA S", "25CSA47"},
        {"JAYA DIVYA J", "25CSA48"},
        {"JAYA HARISH S", "25CSA49"},
        {"JEFRI D", "25CSA50"},
        {"JEGAN R", "25CSA51"},
        {"KABILAN V", "25CSA52"},
        {"KALAIVANI A", "25CSA53"},
        {"KANAKA DURKA T", "25CSA54"},
        {"KANIHA R", "25CSA55"},
        {"KANISH S", "25CSA56"},
        {"KANISHA J", "25CSA57"},
        {"KANISHKA M", "25CSA58"},
        {"KANSHIYA S", "25CSA59"},
        {"KAVISRI S", "25CSA60"},
        {"KAYALVIZHI N", "25CSA61"},
        {"KOUSHIK T", "25CSA62"},
        {"DHARSAN R", "25CSA63"},
        {"KAVIN M", "25CSA64"},
        {"SARAVANAN S", "25CSA65"},
    };

    private final StudentRepository students;
    private final TeacherRepository teachers;

    public DataSeeder(StudentRepository students, TeacherRepository teachers) {
        this.students = students; this.teachers = teachers;
    }

    @Override
    public void run(String... args) {
        for (String[] s : STUDENTS) {
            String reg = s[1].trim().toUpperCase();
            Student st = students.findByRegisterNoIgnoreCase(reg).orElseGet(Student::new);
            st.setName(toTitle(s[0]));
            st.setRegisterNo(reg);
            st.setClassName(CLASS_NAME);
            st.setPasswordHash(PasswordUtil.hash(reg.toLowerCase()));
            students.save(st);
        }
        if (teachers.findByClassName(CLASS_NAME).isEmpty()) {
            teachers.save(new Teacher(CLASS_NAME, PasswordUtil.hash("Arun.M")));
        }
        System.out.println(">> Seed data ready: " + students.count() + " students, " + teachers.count() + " teacher(s)");
    }

    /** "AATHISH B" -> "Aathish B" */
    private static String toTitle(String s) {
        StringBuilder sb = new StringBuilder();
        for (String w : s.trim().split("\\s+")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(w.length() == 1 ? w.toUpperCase()
                    : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
