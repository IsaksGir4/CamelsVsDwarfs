import { useEffect, type ReactNode } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useAuth, type Permission } from "@/lib/auth";
import { LoadingState } from "@/components/states";
import { AccessDenied } from "@/components/AccessDenied";

export function RequireAuth({
  children,
  permission,
}: {
  children: ReactNode;
  permission?: Permission;
}) {
  const { status, hasPermission } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (status === "anonymous" || status === "error") {
      void navigate({ to: "/login", replace: true });
    }
  }, [status, navigate]);

  if (status !== "authenticated") {
    return (
      <div className="p-6">
        <LoadingState rows={4} />
      </div>
    );
  }

  if (permission && !hasPermission(permission)) {
    return <AccessDenied />;
  }

  return <>{children}</>;
}
