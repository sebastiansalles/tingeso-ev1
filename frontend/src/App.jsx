import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Container, CssBaseline } from "@mui/material";
import Navbar from "./components/Navbar";
import RequireRole from "./components/RequireRole";
import Home from "./pages/Home";
import Pending from "./pages/Pending";
import NotFound from "./pages/NotFound";
import CareerList from "./pages/admin/CareerList";
import { ROLES } from "./auth";
import CareerForm from "./pages/admin/CareerForm";

const admin = (page) => <RequireRole roles={[ROLES.ADMIN]}>{page}</RequireRole>;
const teacher = (page) => <RequireRole roles={[ROLES.TEACHER]}>{page}</RequireRole>;
const student = (page) => <RequireRole roles={[ROLES.STUDENT]}>{page}</RequireRole>;

export default function App() {
  return (
    <BrowserRouter>
      <CssBaseline />
      <Navbar />
      <Container sx={{ py: 4 }}>
        <Routes>
          <Route path="/" element={<Home />} />

          {/* Administrator */}
          <Route path="/careers" element={admin(<CareerList />)} />
          <Route path="/subjects" element={admin(<Pending />)} />
          <Route path="/students" element={admin(<Pending />)} />
          <Route path="/teachers" element={admin(<Pending />)} />
          <Route path="/periods" element={admin(<Pending />)} />
          <Route path="/sections" element={admin(<Pending />)} />

          {/* Teacher */}
          <Route path="/my-sections" element={teacher(<Pending />)} />

          {/* Student */}
          <Route path="/my-plan" element={student(<Pending />)} />
          <Route path="/enroll" element={student(<Pending />)} />
          <Route path="/my-enrollments" element={student(<Pending />)} />
          <Route path="/history" element={student(<Pending />)} />

          <Route path="/careers" element={admin(<CareerList />)} />
          <Route path="/careers/new" element={admin(<CareerForm />)} />
          <Route path="/careers/:id/edit" element={admin(<CareerForm />)} />

          <Route path="*" element={<NotFound />} />
        </Routes>
      </Container>
    </BrowserRouter>
  );
}